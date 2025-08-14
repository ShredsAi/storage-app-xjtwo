package ai.shreds.adapters.primary.rest;

import ai.shreds.domain.ports.*;
import ai.shreds.shared.dtos.SharedDTOImageDetails;
import ai.shreds.shared.dtos.SharedDTOPaginatedImages;
import ai.shreds.shared.enums.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
public class AdapterImageControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:14.5")
            .withDatabaseName("image_ingestion_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static final MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z")
            .withUserName("minioadmin")
            .withPassword("minioadmin");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        
        // MinIO configuration
        registry.add("storage.profile", () -> "minio");
        registry.add("storage.minio.endpoint", MINIO::getS3URL);
        registry.add("storage.minio.access-key", MINIO::getUserName);
        registry.add("storage.minio.secret-key", MINIO::getPassword);
        registry.add("storage.minio.bucket-name", () -> "image-storage");
        
        // Security configuration
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/.well-known/jwks.json");
        
        // Test-specific configurations - Fix TaskScheduler poolSize issue
        registry.add("spring.task.scheduling.pool.size", () -> "1");
        registry.add("cleanup.cron", () -> "-");
        registry.add("spring.profiles.active", () -> "test");
    }

    @TestConfiguration
    static class TestSecurityOverrides {
        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> { throw new UnsupportedOperationException("JWT decoding not needed in this test"); };
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DomainServiceMetadataExtractor metadataExtractor;

    @MockBean
    private DomainServiceChecksum checksumService;

    @MockBean
    private DomainOutputPortIdempotencyRepository idempotencyRepository;

    private MinioClient minioClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + port + "/api/v1";
        
        // Initialize MinIO client and create bucket
        minioClient = MinioClient.builder()
                .endpoint(MINIO.getS3URL())
                .credentials(MINIO.getUserName(), MINIO.getPassword())
                .build();
        
        String bucketName = "image-storage";
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
        
        // Reset mocks
        reset(metadataExtractor, checksumService, idempotencyRepository);
    }

    @Test
    void testUploadImageEndToEndWorkflow(CapturedOutput output) throws Exception {
        // Arrange
        UUID itemId = UUID.randomUUID();
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        
        // Create test image data (simple JPEG)
        byte[] imageBytes = createTestJpegBytes();
        
        // Mock external dependencies
        when(idempotencyRepository.findImageIdByKey(idempotencyKey)).thenReturn(Optional.empty());
        
        ai.shreds.shared.value_objects.SharedValueDimensions dimensions = 
                new ai.shreds.shared.value_objects.SharedValueDimensions(1920, 1080);
        ai.shreds.shared.value_objects.SharedValueChecksum checksum = 
                new ai.shreds.shared.value_objects.SharedValueChecksum("SHA-256", "abcdef123456789");
        ai.shreds.shared.value_objects.SharedValueImageMetadata metadata = 
                new ai.shreds.shared.value_objects.SharedValueImageMetadata(
                        SharedEnumImageFormat.JPEG,
                        dimensions,
                        imageBytes.length,
                        checksum
                );
        
        when(metadataExtractor.extract(eq(imageBytes), eq("image/jpeg"), eq(SharedEnumChecksumAlgorithm.SHA_256)))
                .thenReturn(metadata);
        
        ai.shreds.shared.value_objects.SharedValueChecksum sharedChecksum = 
                new ai.shreds.shared.value_objects.SharedValueChecksum("SHA-256", "abcdef123456789");
        when(checksumService.compute(eq(imageBytes), eq(SharedEnumChecksumAlgorithm.SHA_256)))
                .thenReturn(sharedChecksum);
        
        doNothing().when(idempotencyRepository).recordKey(eq(idempotencyKey), any(UUID.class), any(Instant.class));
        
        // Prepare multipart request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Idempotency-Key", idempotencyKey);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        // Add file part
        ByteArrayResource fileResource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "test-image.jpg";
            }
        };
        body.add("file", fileResource);
        
        // Add payload part
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", "Test Image Title");
        payload.put("altText", "Test alt text");
        payload.put("role", "GALLERY");
        payload.put("checksumAlgorithm", "SHA_256");
        payload.put("idempotencyKey", idempotencyKey);
        
        HttpHeaders payloadHeaders = new HttpHeaders();
        payloadHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> payloadEntity = new HttpEntity<>(objectMapper.writeValueAsString(payload), payloadHeaders);
        body.add("payload", payloadEntity);
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        // Act
        ResponseEntity<SharedDTOImageDetails> response = restTemplate.postForEntity(
                baseUrl + "/items/" + itemId + "/images",
                requestEntity,
                SharedDTOImageDetails.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        
        SharedDTOImageDetails result = response.getBody();
        assertThat(result.getImageId()).isNotNull();
        assertThat(result.getItemId()).isEqualTo(itemId);
        assertThat(result.getTitle()).isEqualTo("Test Image Title");
        assertThat(result.getAltText()).isEqualTo("Test alt text");
        assertThat(result.getRole()).isEqualTo(SharedEnumImageRole.GALLERY);
        assertThat(result.getStatus()).isEqualTo(SharedEnumImageStatus.ACTIVE);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        
        // Verify metadata
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata().getFormat()).isEqualTo(SharedEnumImageFormat.JPEG);
        assertThat(result.getMetadata().getDimensions()).isNotNull();
        assertThat(result.getMetadata().getDimensions().getWidth()).isEqualTo(1920);
        assertThat(result.getMetadata().getDimensions().getHeight()).isEqualTo(1080);
        assertThat(result.getMetadata().getFileSizeBytes()).isEqualTo(imageBytes.length);
        assertThat(result.getMetadata().getChecksum()).isNotNull();
        assertThat(result.getMetadata().getChecksum().getAlgorithm()).isEqualTo("SHA-256");
        assertThat(result.getMetadata().getChecksum().getValue()).isEqualTo("abcdef123456789");
        
        // Verify variants
        assertThat(result.getVariants()).isNotNull().isNotEmpty();
        Optional<ai.shreds.shared.dtos.SharedDTOImageVariant> originalVariant = result.getVariants().stream()
                .filter(v -> v.getType() == SharedEnumVariantType.ORIGINAL)
                .findFirst();
        assertThat(originalVariant).isPresent();
        
        ai.shreds.shared.dtos.SharedDTOImageVariant original = originalVariant.get();
        assertThat(original.getFormat()).isEqualTo(SharedEnumImageFormat.JPEG);
        assertThat(original.getDimensions()).isNotNull();
        assertThat(original.getDimensions().getWidth()).isEqualTo(1920);
        assertThat(original.getDimensions().getHeight()).isEqualTo(1080);
        assertThat(original.getFileSizeBytes()).isEqualTo(imageBytes.length);
        assertThat(original.getQuality()).isEqualTo(100);
        assertThat(original.getStorageKey()).contains(result.getImageId().toString());
        assertThat(original.getStorageKey()).endsWith("/original.jpeg");
        
        // Verify external service interactions
        verify(metadataExtractor, times(1)).extract(eq(imageBytes), eq("image/jpeg"), eq(SharedEnumChecksumAlgorithm.SHA_256));
        verify(idempotencyRepository, times(1)).recordKey(eq(idempotencyKey), eq(result.getImageId()), any(Instant.class));
        
        // Verify logs don't contain errors
        String logs = (output.getOut() + "\n" + output.getErr()).toLowerCase();
        assertThat(logs).doesNotContain("application run failed");
        assertThat(logs).doesNotContain(" fatal ");
        assertThat(logs).doesNotContain(" error ");
        
        System.out.println("✅ Upload image end-to-end workflow test completed successfully");
        System.out.println("📊 Created image ID: " + result.getImageId());
        System.out.println("📁 Storage key: " + original.getStorageKey());
    }

    @Test
    void testListImagesWithPaginationAndFiltering(CapturedOutput output) throws Exception {
        // Arrange - First create some test images
        UUID itemId = UUID.randomUUID();
        
        // Create first image
        createTestImageViaAPI(itemId, "First Image", SharedEnumImageRole.PRIMARY);
        
        // Create second image
        createTestImageViaAPI(itemId, "Second Image", SharedEnumImageRole.GALLERY);
        
        // Act - List images with pagination and filtering
        String listUrl = baseUrl + "/items/" + itemId + "/images?status=ACTIVE&role=GALLERY&page=0&size=10&sort=createdAt,desc";
        
        ResponseEntity<SharedDTOPaginatedImages> response = restTemplate.getForEntity(
                listUrl,
                SharedDTOPaginatedImages.class
        );
        
        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        SharedDTOPaginatedImages result = response.getBody();
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(result.getContent()).isNotNull();
        
        // Verify filtering worked - should only contain GALLERY role images
        result.getContent().forEach(image -> {
            assertThat(image.getRole()).isEqualTo(SharedEnumImageRole.GALLERY);
            assertThat(image.getStatus()).isEqualTo(SharedEnumImageStatus.ACTIVE);
            assertThat(image.getItemId()).isEqualTo(itemId);
            assertThat(image.getImageId()).isNotNull();
            assertThat(image.getCreatedAt()).isNotNull();
            assertThat(image.getUpdatedAt()).isNotNull();
            assertThat(image.getVariants()).isNotNull().isNotEmpty();
        });
        
        // Verify logs don't contain errors
        String logs = (output.getOut() + "\n" + output.getErr()).toLowerCase();
        assertThat(logs).doesNotContain("application run failed");
        assertThat(logs).doesNotContain(" fatal ");
        assertThat(logs).doesNotContain(" error ");
        
        System.out.println("✅ List images with pagination and filtering test completed successfully");
        System.out.println("📊 Found " + result.getTotalElements() + " images with GALLERY role");
        System.out.println("📄 Page " + result.getPage() + " of " + result.getTotalPages());
    }
    
    private SharedDTOImageDetails createTestImageViaAPI(UUID itemId, String title, SharedEnumImageRole role) throws Exception {
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        byte[] imageBytes = createTestJpegBytes();
        
        // Mock external dependencies
        when(idempotencyRepository.findImageIdByKey(idempotencyKey)).thenReturn(Optional.empty());
        
        ai.shreds.shared.value_objects.SharedValueDimensions dimensions = 
                new ai.shreds.shared.value_objects.SharedValueDimensions(800, 600);
        ai.shreds.shared.value_objects.SharedValueChecksum checksum = 
                new ai.shreds.shared.value_objects.SharedValueChecksum("SHA-256", "test123456789");
        ai.shreds.shared.value_objects.SharedValueImageMetadata metadata = 
                new ai.shreds.shared.value_objects.SharedValueImageMetadata(
                        SharedEnumImageFormat.JPEG,
                        dimensions,
                        imageBytes.length,
                        checksum
                );
        
        when(metadataExtractor.extract(eq(imageBytes), eq("image/jpeg"), eq(SharedEnumChecksumAlgorithm.SHA_256)))
                .thenReturn(metadata);
        
        ai.shreds.shared.value_objects.SharedValueChecksum sharedChecksum = 
                new ai.shreds.shared.value_objects.SharedValueChecksum("SHA-256", "test123456789");
        when(checksumService.compute(eq(imageBytes), eq(SharedEnumChecksumAlgorithm.SHA_256)))
                .thenReturn(sharedChecksum);
        
        doNothing().when(idempotencyRepository).recordKey(eq(idempotencyKey), any(UUID.class), any(Instant.class));
        
        // Prepare multipart request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Idempotency-Key", idempotencyKey);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        // Add file part
        ByteArrayResource fileResource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return title.toLowerCase().replace(" ", "-") + ".jpg";
            }
        };
        body.add("file", fileResource);
        
        // Add payload part
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("altText", title + " alt text");
        payload.put("role", role.name());
        payload.put("checksumAlgorithm", "SHA_256");
        payload.put("idempotencyKey", idempotencyKey);
        
        HttpHeaders payloadHeaders = new HttpHeaders();
        payloadHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> payloadEntity = new HttpEntity<>(objectMapper.writeValueAsString(payload), payloadHeaders);
        body.add("payload", payloadEntity);
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        ResponseEntity<SharedDTOImageDetails> response = restTemplate.postForEntity(
                baseUrl + "/items/" + itemId + "/images",
                requestEntity,
                SharedDTOImageDetails.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }
    
    /**
     * Creates a minimal valid JPEG byte array for testing.
     */
    private byte[] createTestJpegBytes() {
        // Minimal JPEG header: SOI + APP0 + SOF0 + SOS + EOI
        return new byte[] {
                (byte) 0xFF, (byte) 0xD8, // SOI
                (byte) 0xFF, (byte) 0xE0, // APP0
                0x00, 0x10, // Length
                'J', 'F', 'I', 'F', 0x00, // JFIF identifier
                0x01, 0x01, // Version
                0x01, // Units
                0x00, 0x48, 0x00, 0x48, // X and Y density
                0x00, 0x00, // Thumbnail width and height
                (byte) 0xFF, (byte) 0xC0, // SOF0
                0x00, 0x11, // Length
                0x08, // Precision
                0x04, 0x38, // Height (1080)
                0x07, (byte) 0x80, // Width (1920)
                0x03, // Number of components
                0x01, 0x22, 0x00, // Component 1
                0x02, 0x11, 0x01, // Component 2
                0x03, 0x11, 0x01, // Component 3
                (byte) 0xFF, (byte) 0xDA, // SOS
                0x00, 0x0C, // Length
                0x03, // Number of components
                0x01, 0x00, 0x02, 0x11, 0x03, 0x11, 0x00, 0x3F, 0x00,
                (byte) 0xFF, (byte) 0xD9 // EOI
        };
    }
}
package ai.shreds.application.services;

import ai.shreds.domain.entities.DomainAggregateItemImage;
import ai.shreds.domain.entities.DomainEntityImageProcessingTask;
import ai.shreds.domain.entities.DomainEntityImageVariant;
import ai.shreds.domain.ports.*;
import ai.shreds.domain.services.DomainServiceImageFactory;
import ai.shreds.shared.dtos.SharedDTOImageDetails;
import ai.shreds.shared.dtos.SharedDTOImageUploadPayload;
import ai.shreds.shared.dtos.SharedDTOImageVariant;
import ai.shreds.shared.enums.*;
import ai.shreds.shared.value_objects.SharedValueChecksum;
import ai.shreds.shared.value_objects.SharedValueDimensions;
import ai.shreds.shared.value_objects.SharedValueImageMetadata;
import ai.shreds.shared.value_objects.SharedValueUploadFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(OutputCaptureExtension.class)
public class ApplicationServiceImageIngestionServiceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:14.5")
            .withDatabaseName("image_ingestion_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/.well-known/jwks.json");
        registry.add("storage.profile", () -> "minio");
        registry.add("storage.minio.endpoint", () -> "http://localhost:9000");
        registry.add("storage.minio.access-key", () -> "minioadmin");
        registry.add("storage.minio.secret-key", () -> "minioadmin");
        registry.add("storage.minio.bucket-name", () -> "image-storage");
    }

    @TestConfiguration
    static class TestSecurityOverrides {
        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> { throw new UnsupportedOperationException("JWT decoding not needed in this test"); };
        }
    }

    @Autowired
    private ApplicationServiceImageIngestionService ingestionService;

    @MockBean
    private DomainOutputPortIdempotencyRepository idempotencyRepository;

    @MockBean
    private DomainServiceMetadataExtractor metadataExtractor;

    @MockBean
    private DomainServiceChecksum checksumService;

    @MockBean
    private DomainOutputPortObjectStorage objectStorage;

    @MockBean
    private DomainOutputPortProcessingTaskRepository taskRepository;

    @Autowired
    private DomainOutputPortItemImageRepository itemImageRepository;

    @Autowired
    private DomainOutputPortImageVariantRepository variantRepository;

    @Autowired
    private DomainServiceImageFactory imageFactory;

    @Test
    void testIngestImageCreatesAggregateWithOriginalVariantAndEnqueuesTasks(CapturedOutput output) {
        // Arrange
        UUID itemId = UUID.randomUUID();
        String idempotencyKey = "test-key-" + UUID.randomUUID();
        
        // Create test image data (simple JPEG header)
        byte[] imageBytes = createTestJpegBytes();
        SharedValueUploadFile uploadFile = new SharedValueUploadFile(
                "test-image.jpg",
                "image/jpeg",
                imageBytes.length,
                imageBytes
        );
        
        SharedDTOImageUploadPayload payload = new SharedDTOImageUploadPayload();
        payload.setTitle("Test Image Title");
        payload.setAltText("Test alt text");
        payload.setRole(SharedEnumImageRole.GALLERY);
        payload.setChecksumAlgorithm(SharedEnumChecksumAlgorithm.SHA_256);
        payload.setIdempotencyKey(idempotencyKey);
        
        // Mock external dependencies
        when(idempotencyRepository.findImageIdByKey(idempotencyKey)).thenReturn(Optional.empty());
        
        SharedValueDimensions dimensions = new SharedValueDimensions(1920, 1080);
        SharedValueChecksum checksum = new SharedValueChecksum("SHA-256", "abcdef123456789");
        SharedValueImageMetadata metadata = new SharedValueImageMetadata(
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
        
        doNothing().when(objectStorage).putOriginal(anyString(), eq(imageBytes), eq("image/jpeg"));
        
        when(taskRepository.create(any(DomainEntityImageProcessingTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        doNothing().when(idempotencyRepository).recordKey(eq(idempotencyKey), any(UUID.class), any(Instant.class));
        
        // Act
        SharedDTOImageDetails result = ingestionService.ingestImage(itemId, uploadFile, payload, idempotencyKey);
        
        // Assert
        assertThat(result).isNotNull();
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
        Optional<SharedDTOImageVariant> originalVariant = result.getVariants().stream()
                .filter(v -> v.getType() == SharedEnumVariantType.ORIGINAL)
                .findFirst();
        assertThat(originalVariant).isPresent();
        
        SharedDTOImageVariant original = originalVariant.get();
        assertThat(original.getFormat()).isEqualTo(SharedEnumImageFormat.JPEG);
        assertThat(original.getDimensions()).isNotNull();
        assertThat(original.getDimensions().getWidth()).isEqualTo(1920);
        assertThat(original.getDimensions().getHeight()).isEqualTo(1080);
        assertThat(original.getFileSizeBytes()).isEqualTo(imageBytes.length);
        assertThat(original.getQuality()).isEqualTo(100);
        assertThat(original.getStorageKey()).contains(result.getImageId().toString());
        assertThat(original.getStorageKey()).endsWith("/original.jpeg");
        
        // Verify database persistence
        Optional<DomainAggregateItemImage> persistedImage = itemImageRepository.findById(result.getImageId());
        assertThat(persistedImage).isPresent();
        DomainAggregateItemImage aggregate = persistedImage.get();
        assertThat(aggregate.getImageId()).isEqualTo(result.getImageId());
        assertThat(aggregate.getItemId()).isEqualTo(itemId);
        assertThat(aggregate.getTitle()).isEqualTo("Test Image Title");
        assertThat(aggregate.getAltText()).isEqualTo("Test alt text");
        assertThat(aggregate.getRole()).isEqualTo(SharedEnumImageRole.GALLERY);
        assertThat(aggregate.getStatus()).isEqualTo(SharedEnumImageStatus.ACTIVE);
        
        // Verify external service interactions
        verify(metadataExtractor, times(1)).extract(eq(imageBytes), eq("image/jpeg"), eq(SharedEnumChecksumAlgorithm.SHA_256));
        
        ArgumentCaptor<String> storageKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectStorage, times(1)).putOriginal(storageKeyCaptor.capture(), eq(imageBytes), eq("image/jpeg"));
        String capturedStorageKey = storageKeyCaptor.getValue();
        assertThat(capturedStorageKey).contains(result.getImageId().toString());
        assertThat(capturedStorageKey).endsWith("/original.jpeg");
        
        // Verify processing task creation
        ArgumentCaptor<DomainEntityImageProcessingTask> taskCaptor = ArgumentCaptor.forClass(DomainEntityImageProcessingTask.class);
        verify(taskRepository, times(1)).create(taskCaptor.capture());
        DomainEntityImageProcessingTask capturedTask = taskCaptor.getValue();
        assertThat(capturedTask.getImageId()).isEqualTo(result.getImageId());
        assertThat(capturedTask.getStatus()).isEqualTo(SharedEnumProcessingStatus.PENDING);
        assertThat(capturedTask.toRequestedVariants()).isNotEmpty();
        assertThat(capturedTask.toRequestedVariants()).doesNotContain(SharedEnumVariantType.ORIGINAL);
        
        // Verify idempotency record
        verify(idempotencyRepository, times(1)).recordKey(eq(idempotencyKey), eq(result.getImageId()), any(Instant.class));
        
        // Verify logs don't contain errors
        String logs = (output.getOut() + "\n" + output.getErr()).toLowerCase();
        assertThat(logs).doesNotContain("application run failed");
        assertThat(logs).doesNotContain(" fatal ");
        assertThat(logs).doesNotContain(" error ");
    }

    @Test
    void testIngestImageWithIdempotencyKeyReturnsExistingImage(CapturedOutput output) {
        // Arrange
        UUID itemId = UUID.randomUUID();
        UUID existingImageId = UUID.randomUUID();
        String idempotencyKey = "duplicate-key-" + UUID.randomUUID();
        
        // Create test image data
        byte[] imageBytes = createTestJpegBytes();
        SharedValueUploadFile uploadFile = new SharedValueUploadFile(
                "duplicate-image.jpg",
                "image/jpeg",
                imageBytes.length,
                imageBytes
        );
        
        SharedDTOImageUploadPayload payload = new SharedDTOImageUploadPayload();
        payload.setTitle("Duplicate Image Title");
        payload.setAltText("Duplicate alt text");
        payload.setRole(SharedEnumImageRole.PRIMARY);
        payload.setChecksumAlgorithm(SharedEnumChecksumAlgorithm.SHA_256);
        payload.setIdempotencyKey(idempotencyKey);
        
        // Create existing image in database first
        DomainAggregateItemImage existingImage = createExistingImageInDatabase(existingImageId, itemId);
        
        // Mock idempotency repository to return existing image ID
        when(idempotencyRepository.findImageIdByKey(idempotencyKey))
                .thenReturn(Optional.of(existingImageId));
        
        // Reset all mocks to ensure no side effects
        reset(metadataExtractor, checksumService, objectStorage, taskRepository);
        
        // Act - Call ingestion service with duplicate idempotency key
        SharedDTOImageDetails result = ingestionService.ingestImage(itemId, uploadFile, payload, idempotencyKey);
        
        // Assert - Verify existing image is returned
        assertThat(result).isNotNull();
        assertThat(result.getImageId()).isEqualTo(existingImageId);
        assertThat(result.getItemId()).isEqualTo(itemId);
        assertThat(result.getTitle()).isEqualTo(existingImage.getTitle());
        assertThat(result.getAltText()).isEqualTo(existingImage.getAltText());
        assertThat(result.getRole()).isEqualTo(existingImage.getRole());
        assertThat(result.getStatus()).isEqualTo(SharedEnumImageStatus.ACTIVE);
        
        // Use flexible timestamp comparison to handle nanosecond precision differences
        assertThat(result.getCreatedAt()).isCloseTo(existingImage.getCreatedAt(), within(1, ChronoUnit.SECONDS));
        assertThat(result.getUpdatedAt()).isCloseTo(existingImage.getUpdatedAt(), within(1, ChronoUnit.SECONDS));
        
        // Verify no side effects occurred
        verifyNoInteractions(metadataExtractor);
        verifyNoInteractions(checksumService);
        verifyNoInteractions(objectStorage);
        verifyNoInteractions(taskRepository);
        
        // Verify idempotency repository was only queried, not updated
        verify(idempotencyRepository, times(1)).findImageIdByKey(idempotencyKey);
        verify(idempotencyRepository, never()).recordKey(anyString(), any(UUID.class), any(Instant.class));
        
        // Verify database state unchanged - no new images created
        long imageCount = itemImageRepository.listByItemIdWithFilters(
                itemId, null, null, 0, 100, "createdAt,desc"
        ).getTotalElements();
        assertThat(imageCount).isEqualTo(1); // Only the existing image
        
        // Verify logs don't contain errors
        String logs = (output.getOut() + "\n" + output.getErr()).toLowerCase();
        assertThat(logs).doesNotContain("application run failed");
        assertThat(logs).doesNotContain(" fatal ");
        assertThat(logs).doesNotContain(" error ");
        
        // Verify idempotency behavior in logs
        assertThat(logs).contains("idempotency");
    }
    
    /**
     * Creates an existing image in the database for idempotency testing.
     */
    private DomainAggregateItemImage createExistingImageInDatabase(UUID imageId, UUID itemId) {
        // Create existing image metadata using factory methods
        ai.shreds.domain.value_objects.DomainValueImageMetadata domainMetadata = 
                ai.shreds.domain.value_objects.DomainValueImageMetadata.of(
                        SharedEnumImageFormat.JPEG,
                        ai.shreds.domain.value_objects.DomainValueDimensions.of(800, 600),
                        50000L,
                        ai.shreds.domain.value_objects.DomainValueChecksum.of("SHA-256", "existing123456789")
                );
        
        // Create existing image using factory
        Instant now = Instant.now();
        DomainAggregateItemImage existingImage = imageFactory.createNew(
                imageId,
                itemId,
                SharedEnumImageRole.PRIMARY,
                "Existing Image Title",
                "Existing alt text",
                domainMetadata,
                "images/" + imageId + "/original.jpeg",
                now
        );
        
        // Save to database
        return itemImageRepository.save(existingImage);
    }
    
    /**
     * Creates a minimal valid JPEG byte array for testing.
     * This is a simple JPEG header that should be recognized by image processing libraries.
     */
    private byte[] createTestJpegBytes() {
        // Minimal JPEG header: SOI (Start of Image) + APP0 segment + SOF0 (Start of Frame) + SOS (Start of Scan) + EOI (End of Image)
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
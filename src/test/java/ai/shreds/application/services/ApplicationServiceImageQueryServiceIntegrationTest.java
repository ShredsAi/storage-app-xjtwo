package ai.shreds.application.services;

import ai.shreds.domain.entities.DomainAggregateItemImage;
import ai.shreds.domain.entities.DomainEntityImageVariant;
import ai.shreds.domain.ports.DomainOutputPortItemImageRepository;
import ai.shreds.domain.value_objects.DomainValueChecksum;
import ai.shreds.domain.value_objects.DomainValueDimensions;
import ai.shreds.domain.value_objects.DomainValueImageMetadata;
import ai.shreds.shared.dtos.SharedDTOPaginatedImages;
import ai.shreds.shared.dtos.SharedDTOImageDetails;
import ai.shreds.shared.dtos.SharedDTOImageSummary;
import ai.shreds.shared.dtos.SharedDTOImageVariant;
import ai.shreds.shared.enums.SharedEnumImageFormat;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumImageStatus;
import ai.shreds.shared.enums.SharedEnumVariantType;
import ai.shreds.shared.value_objects.SharedValueListQueryParams;
import ai.shreds.shared.value_objects.SharedValueImageMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(OutputCaptureExtension.class)
public class ApplicationServiceImageQueryServiceIntegrationTest {

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
    private ApplicationServiceImageQueryService queryService;

    @Autowired
    private DomainOutputPortItemImageRepository itemImageRepository;

    @Test
    void When_List_Images_With_Filters_Then_Paginated_Summaries_Returned(CapturedOutput output) {
        UUID itemId = UUID.randomUUID();

        // Seed images with varying roles/statuses
        DomainAggregateItemImage g1 = persistAggregate(itemId, SharedEnumImageRole.GALLERY, SharedEnumImageStatus.ACTIVE, SharedEnumImageFormat.JPEG,
                "Gallery Active 1", "alt1");
        DomainAggregateItemImage g2 = persistAggregate(itemId, SharedEnumImageRole.GALLERY, SharedEnumImageStatus.ACTIVE, SharedEnumImageFormat.JPEG,
                "Gallery Active 2", "alt2");
        DomainAggregateItemImage p1 = persistAggregate(itemId, SharedEnumImageRole.PRIMARY, SharedEnumImageStatus.ACTIVE, SharedEnumImageFormat.PNG,
                "Primary Active", "primary alt");
        DomainAggregateItemImage g3 = persistAggregate(itemId, SharedEnumImageRole.GALLERY, SharedEnumImageStatus.INACTIVE, SharedEnumImageFormat.JPEG,
                "Gallery Inactive", "alt3");

        assertThat(g1).isNotNull();
        assertThat(g2).isNotNull();
        assertThat(p1).isNotNull();
        assertThat(g3).isNotNull();

        SharedValueListQueryParams params = new SharedValueListQueryParams();
        params.setStatus("ACTIVE");
        params.setRole(SharedEnumImageRole.GALLERY);
        params.setPage(0);
        params.setSize(2);
        params.setSort("createdAt,desc");

        SharedDTOPaginatedImages result = queryService.listImages(itemId, params);

        assertThat(result).isNotNull();
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(2); // only g1 and g2 match
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getContent()).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        for (SharedDTOImageSummary summary : result.getContent()) {
            assertThat(summary.getImageId()).isNotNull();
            assertThat(summary.getItemId()).isEqualTo(itemId);
            assertThat(summary.getRole()).isEqualTo(SharedEnumImageRole.GALLERY);
            assertThat(summary.getStatus()).isEqualTo(SharedEnumImageStatus.ACTIVE);
            assertThat(summary.getCreatedAt()).isNotNull();
            assertThat(summary.getUpdatedAt()).isNotNull();
            assertThat(summary.getVariants()).isNotEmpty();

            Optional<SharedDTOImageVariant> originalOpt = summary.getVariants().stream()
                    .filter(v -> v.getType() == SharedEnumVariantType.ORIGINAL)
                    .findFirst();
            assertThat(originalOpt).isPresent();
            SharedDTOImageVariant original = originalOpt.get();
            String expectedKeySuffix = "/original." + (original.getFormat() == SharedEnumImageFormat.JPEG ? "jpeg" : original.getFormat().name().toLowerCase());
            assertThat(original.getStorageKey()).endsWith(expectedKeySuffix);
        }

        String logs = (output.getOut() + "\n" + output.getErr()).toLowerCase();
        assertThat(logs).doesNotContain("application run failed");
    }

    @Test
    void When_Get_Image_By_Id_Then_Full_Details_Including_Variants_Returned(CapturedOutput output) {
        UUID itemId = UUID.randomUUID();
        SharedEnumImageFormat originalFormat = SharedEnumImageFormat.WEBP;
        String title = "Primary Title";
        String alt = "Accessible text";

        DomainAggregateItemImage aggregate = persistAggregate(
                itemId,
                SharedEnumImageRole.PRIMARY,
                SharedEnumImageStatus.ACTIVE,
                originalFormat,
                title,
                alt
        );

        SharedDTOImageDetails details = queryService.getImage(aggregate.getImageId());
        assertThat(details).isNotNull();
        assertThat(details.getImageId()).isEqualTo(aggregate.getImageId());
        assertThat(details.getItemId()).isEqualTo(itemId);
        assertThat(details.getRole()).isEqualTo(SharedEnumImageRole.PRIMARY);
        assertThat(details.getTitle()).isEqualTo(title);
        assertThat(details.getAltText()).isEqualTo(alt);
        assertThat(details.getStatus()).isEqualTo(SharedEnumImageStatus.ACTIVE);
        assertThat(details.getCreatedAt()).isNotNull();
        assertThat(details.getUpdatedAt()).isNotNull();

        SharedValueImageMetadata md = details.getMetadata();
        assertThat(md).isNotNull();
        assertThat(md.getFormat()).isEqualTo(originalFormat);
        assertThat(md.getDimensions()).isNotNull();
        assertThat(md.getDimensions().getWidth()).isEqualTo(1200);
        assertThat(md.getDimensions().getHeight()).isEqualTo(800);
        assertThat(md.getFileSizeBytes()).isEqualTo(1024L);
        assertThat(md.getChecksum()).isNotNull();
        assertThat(md.getChecksum().getAlgorithm()).isEqualTo("SHA-256");
        assertThat(md.getChecksum().getValue()).isNotBlank();

        assertThat(details.getVariants()).isNotNull().isNotEmpty();
        Optional<SharedDTOImageVariant> originalOpt = details.getVariants().stream()
                .filter(v -> v.getType() == SharedEnumVariantType.ORIGINAL)
                .findFirst();
        assertThat(originalOpt).isPresent();
        SharedDTOImageVariant original = originalOpt.get();
        assertThat(original.getFormat()).isEqualTo(originalFormat);
        assertThat(original.getDimensions()).isNotNull();
        assertThat(original.getDimensions().getWidth()).isEqualTo(1200);
        assertThat(original.getDimensions().getHeight()).isEqualTo(800);
        assertThat(original.getFileSizeBytes()).isEqualTo(1024L);
        String expectedKeySuffix = "/original." + (originalFormat == SharedEnumImageFormat.JPEG ? "jpeg" : originalFormat.name().toLowerCase());
        assertThat(original.getStorageKey()).endsWith(expectedKeySuffix);

        String logs = (output.getOut() + "\n" + output.getErr()).toLowerCase();
        assertThat(logs).doesNotContain("application run failed");
    }

    private DomainAggregateItemImage persistAggregate(UUID itemId,
                                                      SharedEnumImageRole role,
                                                      SharedEnumImageStatus status,
                                                      SharedEnumImageFormat originalFormat,
                                                      String title,
                                                      String altText) {
        UUID imageId = UUID.randomUUID();
        Instant now = Instant.now();
        DomainValueDimensions dims = DomainValueDimensions.of(1200, 800);
        long fileSize = 1024L;
        DomainValueChecksum checksum = DomainValueChecksum.of("SHA-256", UUID.randomUUID().toString().replace("-", ""));
        DomainValueImageMetadata metadata = DomainValueImageMetadata.of(originalFormat, dims, fileSize, checksum);

        String ext = originalFormat == SharedEnumImageFormat.JPEG ? "jpeg" : originalFormat.name().toLowerCase();
        String storageKey = "images/" + imageId + "/original." + ext;

        DomainEntityImageVariant original = new DomainEntityImageVariant(
                UUID.randomUUID(),
                imageId,
                SharedEnumVariantType.ORIGINAL,
                originalFormat,
                dims,
                fileSize,
                100,
                storageKey
        );

        DomainAggregateItemImage aggregate = new DomainAggregateItemImage(
                imageId,
                itemId,
                role,
                title,
                // alt text rule: PRIMARY requires non-blank
                role == SharedEnumImageRole.PRIMARY ? (altText == null ? "alt" : altText) : altText,
                status,
                now.minusSeconds(60),
                now,
                metadata,
                Set.of(original),
                Set.of()
        );

        return itemImageRepository.save(aggregate);
    }
}

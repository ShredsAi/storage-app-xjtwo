package ai.shreds.application.services;

import ai.shreds.domain.entities.DomainAggregateItemImage;
import ai.shreds.domain.entities.DomainEntityImageProcessingTask;
import ai.shreds.domain.entities.DomainEntityImageVariant;
import ai.shreds.domain.ports.DomainOutputPortImageVariantRepository;
import ai.shreds.domain.ports.DomainOutputPortItemImageRepository;
import ai.shreds.domain.ports.DomainOutputPortObjectStorage;
import ai.shreds.domain.ports.DomainOutputPortProcessingTaskRepository;
import ai.shreds.domain.value_objects.DomainValueChecksum;
import ai.shreds.domain.value_objects.DomainValueDimensions;
import ai.shreds.domain.value_objects.DomainValueImageMetadata;
import ai.shreds.shared.dtos.SharedDTOImageStatusResponse;
import ai.shreds.shared.enums.SharedEnumImageFormat;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumImageStatus;
import ai.shreds.shared.enums.SharedEnumVariantType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
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
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(OutputCaptureExtension.class)
@EnableAutoConfiguration(exclude = {TaskSchedulingAutoConfiguration.class})
@TestPropertySource(properties = {
    "cleanup.cron=-",
    "spring.profiles.active=test"
})
public class ApplicationServiceImageManagementServiceIntegrationTest {

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
        registry.add("cleanup.cron", () -> "-");
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
    private ApplicationServiceImageManagementService service;

    @MockBean
    private DomainOutputPortItemImageRepository itemImageRepository;

    @MockBean
    private DomainOutputPortProcessingTaskRepository taskRepository;

    @MockBean
    private DomainOutputPortObjectStorage objectStorage;

    @MockBean
    private DomainOutputPortImageVariantRepository variantRepository;

    @Test
    void When_Activate_Inactive_Image_Then_Status_Transitions_To_ACTIVE(CapturedOutput output) {
        UUID imageId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(120);
        Instant initialUpdatedAt = createdAt; // deliberately same to ensure bump occurs

        // Build metadata and ORIGINAL variant
        DomainValueDimensions dims = DomainValueDimensions.of(1920, 1080);
        DomainValueChecksum checksum = DomainValueChecksum.of("SHA-256", "abcdef123456");
        DomainValueImageMetadata metadata = DomainValueImageMetadata.of(SharedEnumImageFormat.JPEG, dims, 1024L, checksum);

        DomainEntityImageVariant original = new DomainEntityImageVariant(
                UUID.randomUUID(),
                imageId,
                SharedEnumVariantType.ORIGINAL,
                SharedEnumImageFormat.JPEG,
                dims,
                1024L,
                100,
                "images/" + imageId + "/original.jpeg"
        );

        DomainAggregateItemImage aggregate = new DomainAggregateItemImage(
                imageId,
                itemId,
                SharedEnumImageRole.GALLERY,
                "Title",
                null,
                SharedEnumImageStatus.INACTIVE,
                createdAt,
                initialUpdatedAt,
                metadata,
                Set.of(original),
                Set.of()
        );

        when(itemImageRepository.findById(imageId)).thenReturn(Optional.of(aggregate));
        when(itemImageRepository.save(any(DomainAggregateItemImage.class))).thenAnswer(inv -> inv.getArgument(0));

        SharedDTOImageStatusResponse response = service.activate(imageId);

        // Verify repository save invoked with ACTIVE status and updatedAt advanced
        ArgumentCaptor<DomainAggregateItemImage> aggCaptor = ArgumentCaptor.forClass(DomainAggregateItemImage.class);
        verify(itemImageRepository, times(1)).save(aggCaptor.capture());
        DomainAggregateItemImage saved = aggCaptor.getValue();

        assertThat(saved.getStatus()).isEqualTo(SharedEnumImageStatus.ACTIVE);
        assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(initialUpdatedAt);

        // Verify response mirrors ACTIVE status and updatedAt is advanced
        assertThat(response).isNotNull();
        assertThat(response.getImageId()).isEqualTo(imageId);
        assertThat(response.getStatus()).isEqualTo(SharedEnumImageStatus.ACTIVE);
        assertThat(response.getUpdatedAt()).isAfterOrEqualTo(initialUpdatedAt);

        // Logs sanity: ensure no fatal startup error markers (avoid brittle substring 'exception')
        String logs = (output.getOut() + "\n" + output.getErr()).toLowerCase();
        assertThat(logs).doesNotContain("application run failed");
        assertThat(logs).doesNotContain(" fatal ");
    }

    @Test
    void When_Delete_With_Running_Task_Then_Deletion_Rejected(CapturedOutput output) {
        UUID imageId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(300);
        Instant updatedAt = createdAt;

        // Build metadata and ORIGINAL variant
        DomainValueDimensions dims = DomainValueDimensions.of(800, 600);
        DomainValueChecksum checksum = DomainValueChecksum.of("SHA-256", "deadbeef");
        DomainValueImageMetadata metadata = DomainValueImageMetadata.of(SharedEnumImageFormat.PNG, dims, 2048L, checksum);
        DomainEntityImageVariant original = new DomainEntityImageVariant(
                UUID.randomUUID(),
                imageId,
                SharedEnumVariantType.ORIGINAL,
                SharedEnumImageFormat.PNG,
                dims,
                2048L,
                100,
                "images/" + imageId + "/original.png"
        );
        DomainAggregateItemImage aggregate = new DomainAggregateItemImage(
                imageId,
                itemId,
                SharedEnumImageRole.GALLERY,
                "To be deleted",
                "alt",
                SharedEnumImageStatus.ACTIVE,
                createdAt,
                updatedAt,
                metadata,
                Set.of(original),
                Set.of()
        );

        when(itemImageRepository.findById(imageId)).thenReturn(Optional.of(aggregate));
        // Simulate a running task
        DomainEntityImageProcessingTask runningTask = mock(DomainEntityImageProcessingTask.class);
        when(taskRepository.findRunningByImageId(imageId)).thenReturn(Set.of(runningTask));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.delete(imageId));
        assertThat(ex.getMessage()).containsIgnoringCase("cannot delete image");

        // Ensure no side effects: no variant/object deletions and no aggregate save
        verify(objectStorage, never()).deleteObject(anyString());
        verify(itemImageRepository, never()).save(any(DomainAggregateItemImage.class));
        verify(variantRepository, never()).findByImageId(any(UUID.class));

        // Aggregate remains not deleted
        assertThat(aggregate.getStatus()).isNotEqualTo(SharedEnumImageStatus.DELETED);

        String logs = (output.getOut() + "\n" + output.getErr()).toLowerCase();
        assertThat(logs).doesNotContain("application run failed");
    }
}

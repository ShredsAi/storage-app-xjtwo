package ai.shreds.infrastructure.repositories.mappers;

import ai.shreds.infrastructure.config.InfrastructureJpaEnumConverters;
import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaImageProcessingTaskEntity;
import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaImageVariantEntity;
import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaItemImageEntity;
import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaTaskRequestedVariantEntity;
import ai.shreds.domain.entities.DomainAggregateItemImage;
import ai.shreds.domain.entities.DomainEntityImageProcessingTask;
import ai.shreds.domain.entities.DomainEntityImageVariant;
import ai.shreds.domain.value_objects.DomainValueChecksum;
import ai.shreds.domain.value_objects.DomainValueDimensions;
import ai.shreds.domain.value_objects.DomainValueImageMetadata;
import ai.shreds.domain.value_objects.DomainValuePage;
import ai.shreds.shared.enums.SharedEnumImageFormat;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumImageStatus;
import ai.shreds.shared.enums.SharedEnumProcessingStatus;
import ai.shreds.shared.enums.SharedEnumVariantType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class InfrastructureMapperJpaToDomain {

    private final InfrastructureJpaEnumConverters converters;

    public InfrastructureMapperJpaToDomain(InfrastructureJpaEnumConverters converters) {
        this.converters = converters;
    }

    // --------------------------- JPA -> Domain ---------------------------
    public DomainAggregateItemImage toDomainAggregate(
            InfrastructureJpaItemImageEntity img,
            List<InfrastructureJpaImageVariantEntity> variants,
            List<InfrastructureJpaImageProcessingTaskEntity> tasks,
            List<InfrastructureJpaTaskRequestedVariantEntity> requested
    ) {
        if (img == null) return null;
        UUID imageId = img.getImageId();
        UUID itemId = img.getItemId();
        SharedEnumImageRole role = converters.fromDbImageRole(img.getRole());
        String title = img.getTitle();
        String altText = img.getAltText();
        SharedEnumImageStatus status = converters.fromDbImageStatus(img.getStatus());
        Instant createdAt = img.getCreatedAt();
        Instant updatedAt = img.getUpdatedAt();
        DomainValueDimensions dims = DomainValueDimensions.of(img.getWidth(), img.getHeight());
        DomainValueChecksum checksum = DomainValueChecksum.of(img.getChecksumAlgorithm(), img.getChecksumValue());
        SharedEnumImageFormat fmt = converters.fromDbImageFormat(img.getFormat());
        DomainValueImageMetadata metadata = DomainValueImageMetadata.of(fmt, dims, img.getFileSizeBytes(), checksum);

        Map<SharedEnumVariantType, DomainEntityImageVariant> domainVariants = new EnumMap<>(SharedEnumVariantType.class);
        for (InfrastructureJpaImageVariantEntity v : variants) {
            DomainEntityImageVariant dv = toDomainVariant(v);
            domainVariants.put(dv.getType(), dv);
        }

        Map<UUID, Set<SharedEnumVariantType>> reqByTask = requested.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getId().getTaskId(),
                        Collectors.mapping(e -> SharedEnumVariantType.valueOf(e.getId().getVariantType()), Collectors.toSet())
                ));
        List<DomainEntityImageProcessingTask> domainTasks = new ArrayList<>();
        for (InfrastructureJpaImageProcessingTaskEntity t : tasks) {
            Set<SharedEnumVariantType> req = reqByTask.getOrDefault(t.getTaskId(), Collections.emptySet());
            domainTasks.add(toDomainTask(t, req));
        }

        return new DomainAggregateItemImage(
                imageId, itemId, role, title, altText, status, createdAt, updatedAt,
                metadata, new HashSet<>(domainVariants.values()), new HashSet<>(domainTasks)
        );
    }

    public DomainEntityImageVariant toDomainVariant(InfrastructureJpaImageVariantEntity entity) {
        UUID variantId = entity.getVariantId();
        UUID imageId = entity.getImageId();
        SharedEnumVariantType type = SharedEnumVariantType.valueOf(entity.getType());
        SharedEnumImageFormat format = converters.fromDbImageFormat(entity.getFormat());
        DomainValueDimensions dims = DomainValueDimensions.of(entity.getWidth(), entity.getHeight());
        long size = entity.getFileSizeBytes();
        int quality = entity.getQuality();
        String storageKey = entity.getStorageKey();
        return new DomainEntityImageVariant(variantId, imageId, type, format, dims, size, quality, storageKey);
    }

    public DomainEntityImageProcessingTask toDomainTask(InfrastructureJpaImageProcessingTaskEntity entity,
                                                        Set<SharedEnumVariantType> requested) {
        SharedEnumProcessingStatus status = SharedEnumProcessingStatus.valueOf(entity.getStatus());
        Set<SharedEnumVariantType> req = (requested == null || requested.isEmpty())
                ? EnumSet.noneOf(SharedEnumVariantType.class)
                : EnumSet.copyOf(requested);
        // Build domain task directly from persisted state using the proper constructor
        return new DomainEntityImageProcessingTask(
                entity.getTaskId(),
                entity.getImageId(),
                status,
                entity.getRequestedAt(),
                entity.getCompletedAt(),
                req
        );
    }

    public DomainValuePage<DomainAggregateItemImage> toDomainPage(int page, int size, long totalElements, int totalPages,
                                        List<DomainAggregateItemImage> content) {
        return DomainValuePage.of(page, size, totalElements, totalPages, new ArrayList<>(content));
    }

    // --------------------------- Domain -> JPA ---------------------------
    public InfrastructureJpaItemImageEntity toJpaItemImage(DomainAggregateItemImage agg) {
        InfrastructureJpaItemImageEntity e = new InfrastructureJpaItemImageEntity();
        e.setImageId(agg.getImageId());
        e.setItemId(agg.getItemId());
        e.setRole(converters.toDbImageRole(agg.getRole()));
        e.setTitle(agg.getTitle());
        e.setAltText(agg.getAltText());
        e.setStatus(converters.toDbImageStatus(agg.getStatus()));
        e.setCreatedAt(agg.getCreatedAt());
        e.setUpdatedAt(agg.getUpdatedAt());
        e.setFormat(converters.toDbImageFormat(agg.getMetadata().getFormat()));
        e.setWidth(agg.getMetadata().getDimensions().getWidth());
        e.setHeight(agg.getMetadata().getDimensions().getHeight());
        e.setFileSizeBytes(agg.getMetadata().getFileSizeBytes());
        e.setChecksumAlgorithm(agg.getMetadata().getChecksum().normalizedAlgorithm());
        e.setChecksumValue(agg.getMetadata().getChecksum().getValue());
        return e;
    }

    public List<InfrastructureJpaImageVariantEntity> toJpaVariants(DomainAggregateItemImage agg) {
        return agg.getVariants().stream().map(v -> {
            InfrastructureJpaImageVariantEntity e = new InfrastructureJpaImageVariantEntity();
            e.setVariantId(v.getVariantId());
            e.setImageId(v.getImageId());
            e.setType(v.getType().name());
            e.setFormat(converters.toDbImageFormat(v.getFormat()));
            e.setWidth(v.getDimensions().getWidth());
            e.setHeight(v.getDimensions().getHeight());
            e.setFileSizeBytes(v.getFileSizeBytes());
            e.setQuality(v.getQuality());
            e.setStorageKey(v.getStorageKey());
            return e;
        }).collect(Collectors.toList());
    }

    public InfrastructureJpaImageProcessingTaskEntity toJpaTask(DomainEntityImageProcessingTask task) {
        InfrastructureJpaImageProcessingTaskEntity e = new InfrastructureJpaImageProcessingTaskEntity();
        e.setTaskId(task.getTaskId());
        e.setImageId(task.getImageId());
        e.setStatus(task.getStatus().name());
        e.setRequestedAt(task.getRequestedAt());
        e.setCompletedAt(task.getCompletedAt());
        return e;
    }

    public List<InfrastructureJpaTaskRequestedVariantEntity> toJpaRequestedVariants(UUID taskId, Set<SharedEnumVariantType> variants) {
        return variants.stream()
                .map(v -> new InfrastructureJpaTaskRequestedVariantEntity(taskId, v.name()))
                .collect(Collectors.toList());
    }
}

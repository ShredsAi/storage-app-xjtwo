package ai.shreds.shared.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ai.shreds.shared.dtos.SharedDTOImageDetails;
import ai.shreds.shared.dtos.SharedDTOImageSummary;
import ai.shreds.shared.dtos.SharedDTOImageVariant;
import ai.shreds.shared.dtos.SharedDTOProcessingTaskSummary;
import ai.shreds.shared.value_objects.SharedValueChecksum;
import ai.shreds.shared.value_objects.SharedValueDimensions;
import ai.shreds.shared.value_objects.SharedValueImageMetadata;
import ai.shreds.shared.enums.SharedEnumImageFormat;
import ai.shreds.shared.enums.SharedEnumVariantType;

import ai.shreds.domain.entities.DomainAggregateItemImage;
import ai.shreds.domain.entities.DomainEntityImageVariant;
import ai.shreds.domain.entities.DomainEntityImageProcessingTask;
import ai.shreds.domain.value_objects.DomainValueDimensions;
import ai.shreds.domain.value_objects.DomainValueImageMetadata;
import ai.shreds.domain.value_objects.DomainValueChecksum;

public final class SharedUtilImageDtoMapper {

    private SharedUtilImageDtoMapper() {
    }

    public static SharedDTOImageDetails toImageDetails(DomainAggregateItemImage image) {
        Objects.requireNonNull(image, "image");
        SharedDTOImageDetails dto = new SharedDTOImageDetails();
        dto.setImageId(image.getImageId());
        dto.setItemId(image.getItemId());
        dto.setRole(image.getRole());
        dto.setTitle(image.getTitle());
        dto.setAltText(image.getAltText());
        dto.setStatus(image.getStatus());
        dto.setCreatedAt(image.getCreatedAt());
        dto.setUpdatedAt(image.getUpdatedAt());
        dto.setMetadata(toSharedMetadata(image.getMetadata()));
        dto.setVariants(toVariantList(image.getVariants()));
        return dto;
    }

    public static SharedDTOImageSummary toImageSummary(DomainAggregateItemImage image) {
        Objects.requireNonNull(image, "image");
        SharedDTOImageSummary dto = new SharedDTOImageSummary();
        dto.setImageId(image.getImageId());
        dto.setItemId(image.getItemId());
        dto.setRole(image.getRole());
        dto.setTitle(image.getTitle());
        dto.setAltText(image.getAltText());
        dto.setStatus(image.getStatus());
        dto.setCreatedAt(image.getCreatedAt());
        dto.setUpdatedAt(image.getUpdatedAt());
        dto.setVariants(toVariantList(image.getVariants()));
        return dto;
    }

    public static SharedDTOImageVariant toImageVariant(DomainEntityImageVariant variant) {
        Objects.requireNonNull(variant, "variant");
        SharedDTOImageVariant dto = new SharedDTOImageVariant();
        dto.setType(variant.getType());
        dto.setFormat(variant.getFormat());
        dto.setDimensions(toSharedDimensions(variant.getDimensions()));
        dto.setFileSizeBytes(variant.getFileSizeBytes());
        dto.setQuality(variant.getQuality());
        dto.setStorageKey(variant.getStorageKey());
        return dto;
    }

    public static SharedDTOProcessingTaskSummary toProcessingTaskSummary(DomainEntityImageProcessingTask task) {
        Objects.requireNonNull(task, "task");
        SharedDTOProcessingTaskSummary dto = new SharedDTOProcessingTaskSummary();
        dto.setTaskId(task.getTaskId());
        dto.setStatus(task.getStatus());
        dto.setRequestedAt(task.getRequestedAt());
        dto.setCompletedAt(task.getCompletedAt());
        return dto;
    }

    private static List<SharedDTOImageVariant> toVariantList(Set<DomainEntityImageVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return new ArrayList<>();
        }
        return variants.stream()
                .sorted(Comparator.comparing(DomainEntityImageVariant::getType, SharedUtilImageDtoMapper::compareVariantType))
                .map(SharedUtilImageDtoMapper::toImageVariant)
                .collect(Collectors.toList());
    }

    private static int compareVariantType(SharedEnumVariantType a, SharedEnumVariantType b) {
        // Provide stable order ORIGINAL, THUMBNAIL, SMALL, MEDIUM, LARGE
        List<SharedEnumVariantType> order = List.of(
                SharedEnumVariantType.ORIGINAL,
                SharedEnumVariantType.THUMBNAIL,
                SharedEnumVariantType.SMALL,
                SharedEnumVariantType.MEDIUM,
                SharedEnumVariantType.LARGE
        );
        return Integer.compare(order.indexOf(a), order.indexOf(b));
    }

    private static SharedValueImageMetadata toSharedMetadata(DomainValueImageMetadata metadata) {
        if (metadata == null) return null;
        SharedEnumImageFormat fmt = metadata.getFormat();
        SharedValueDimensions dims = toSharedDimensions(metadata.getDimensions());
        long size = metadata.getFileSizeBytes();
        SharedValueChecksum checksum = toSharedChecksum(metadata.getChecksum());
        return new SharedValueImageMetadata(fmt, dims, size, checksum);
    }

    private static SharedValueDimensions toSharedDimensions(DomainValueDimensions d) {
        if (d == null) return null;
        return new SharedValueDimensions(d.getWidth(), d.getHeight());
    }

    private static SharedValueChecksum toSharedChecksum(DomainValueChecksum c) {
        if (c == null) return null;
        return new SharedValueChecksum(c.getAlgorithm(), c.getValue());
    }
}

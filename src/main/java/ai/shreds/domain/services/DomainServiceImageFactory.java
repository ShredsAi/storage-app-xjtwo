package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainAggregateItemImage;
import ai.shreds.domain.entities.DomainEntityImageVariant;
import ai.shreds.domain.exceptions.DomainExceptionValidation;
import ai.shreds.domain.value_objects.DomainValueImageMetadata;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumImageStatus;
import ai.shreds.shared.enums.SharedEnumVariantType;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Factory for creating a new ItemImage aggregate with the mandatory ORIGINAL variant from provided metadata.
 */
public class DomainServiceImageFactory {

    public DomainAggregateItemImage createNew(UUID imageId,
                                              UUID itemId,
                                              SharedEnumImageRole role,
                                              String title,
                                              String altText,
                                              DomainValueImageMetadata metadata,
                                              String originalStorageKey,
                                              Instant now) {
        if (imageId == null) throw new DomainExceptionValidation("imageId must not be null");
        if (itemId == null) throw new DomainExceptionValidation("itemId must not be null");
        if (role == null) throw new DomainExceptionValidation("role must not be null");
        if (metadata == null) throw new DomainExceptionValidation("metadata must not be null");
        if (originalStorageKey == null || originalStorageKey.isBlank()) {
            throw new DomainExceptionValidation("originalStorageKey must not be blank");
        }
        Instant ts = (now == null) ? Instant.now() : now;

        // Build ORIGINAL variant matching metadata
        DomainEntityImageVariant original = new DomainEntityImageVariant(
                UUID.randomUUID(),
                imageId,
                SharedEnumVariantType.ORIGINAL,
                metadata.getFormat(),
                metadata.getDimensions(),
                metadata.getFileSizeBytes(),
                100,
                originalStorageKey
        );

        // Create aggregate with ACTIVE by default on creation
        return new DomainAggregateItemImage(
                imageId,
                itemId,
                role,
                title,
                altText,
                SharedEnumImageStatus.ACTIVE,
                ts,
                ts,
                metadata,
                List.of(original),
                Set.of()
        );
    }
}

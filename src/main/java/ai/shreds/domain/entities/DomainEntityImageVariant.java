package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import ai.shreds.domain.exceptions.DomainExceptionValidation;
import ai.shreds.domain.value_objects.DomainValueDimensions;
import ai.shreds.shared.enums.SharedEnumImageFormat;
import ai.shreds.shared.enums.SharedEnumVariantType;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Image variant entity representing a specific rendition of an image.
 */
public class DomainEntityImageVariant {
    private UUID variantId;
    private UUID imageId;
    private SharedEnumVariantType type;
    private SharedEnumImageFormat format;
    private DomainValueDimensions dimensions;
    private long fileSizeBytes;
    private int quality;
    private String storageKey;

    public DomainEntityImageVariant(UUID variantId,
                                    UUID imageId,
                                    SharedEnumVariantType type,
                                    SharedEnumImageFormat format,
                                    DomainValueDimensions dimensions,
                                    long fileSizeBytes,
                                    int quality,
                                    String storageKey) {
        if (variantId == null) throw new DomainExceptionValidation("variantId must be provided");
        if (imageId == null) throw new DomainExceptionValidation("imageId must be provided");
        if (type == null) throw new DomainExceptionValidation("variant type must be provided");
        if (format == null) throw new DomainExceptionValidation("format must be provided");
        if (dimensions == null) throw new DomainExceptionValidation("dimensions must be provided");
        if (fileSizeBytes <= 0) throw new DomainExceptionValidation("fileSizeBytes must be > 0");
        if (quality < 0 || quality > 100) throw new DomainExceptionValidation("quality must be in [0,100]");
        if (storageKey == null || storageKey.isBlank()) throw new DomainExceptionValidation("storageKey must be provided");
        this.variantId = variantId;
        this.imageId = imageId;
        this.type = type;
        this.format = format;
        this.dimensions = dimensions;
        this.fileSizeBytes = fileSizeBytes;
        this.quality = quality;
        this.storageKey = storageKey;
    }

    public UUID getVariantId() { return variantId; }
    public UUID getImageId() { return imageId; }
    public SharedEnumVariantType getType() { return type; }
    public SharedEnumImageFormat getFormat() { return format; }
    public DomainValueDimensions getDimensions() { return dimensions; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public int getQuality() { return quality; }
    public String getStorageKey() { return storageKey; }

    public void updateQuality(int newQuality) {
        if (type == SharedEnumVariantType.ORIGINAL) {
            throw new DomainExceptionInvariantViolation("Cannot update quality of ORIGINAL variant");
        }
        int q = Math.max(0, Math.min(100, newQuality));
        this.quality = q;
    }

    /**
     * Apply post-processing revision to non-ORIGINAL variant.
     */
    public void reviseAfterReprocess(SharedEnumImageFormat newFormat,
                                     DomainValueDimensions newDimensions,
                                     long newSize,
                                     int newQuality) {
        if (type == SharedEnumVariantType.ORIGINAL) {
            throw new DomainExceptionInvariantViolation("ORIGINAL variant is immutable");
        }
        if (newFormat == null) throw new DomainExceptionValidation("new format must be provided");
        if (newDimensions == null) throw new DomainExceptionValidation("new dimensions must be provided");
        if (newSize <= 0) throw new DomainExceptionValidation("new size must be > 0");
        if (newQuality < 0 || newQuality > 100) throw new DomainExceptionValidation("new quality must be in [0,100]");
        this.format = newFormat;
        this.dimensions = newDimensions;
        this.fileSizeBytes = newSize;
        this.quality = newQuality;
    }

    /**
     * Determine if this variant can serve a request of target size and preferred formats.
     */
    public boolean canServeFor(int targetWidth, int targetHeight, Set<SharedEnumImageFormat> preferredFormats) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new DomainExceptionValidation("Target width/height must be > 0");
        }
        boolean fits = dimensions.getWidth() >= targetWidth && dimensions.getHeight() >= targetHeight;
        boolean formatOk = preferredFormats == null || preferredFormats.isEmpty() || preferredFormats.contains(this.format);
        return fits && formatOk;
    }

    public DomainValueDimensions toDimensions() {
        return this.dimensions;
    }

    /**
     * Update the storage key reference for this variant.
     */
    public void setStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new DomainExceptionValidation("storageKey must be provided");
        }
        this.storageKey = storageKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainEntityImageVariant)) return false;
        DomainEntityImageVariant that = (DomainEntityImageVariant) o;
        return Objects.equals(variantId, that.variantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variantId);
    }
}

package ai.shreds.shared.dtos;

import ai.shreds.shared.enums.SharedEnumImageFormat;
import ai.shreds.shared.enums.SharedEnumVariantType;
import ai.shreds.shared.value_objects.SharedValueDimensions;

public class SharedDTOImageVariant {
    private SharedEnumVariantType type;
    private SharedEnumImageFormat format;
    private SharedValueDimensions dimensions;
    private long fileSizeBytes;
    private int quality;
    private String storageKey;

    public SharedEnumVariantType getType() {
        return type;
    }

    public void setType(SharedEnumVariantType type) {
        this.type = type;
    }

    public SharedEnumImageFormat getFormat() {
        return format;
    }

    public void setFormat(SharedEnumImageFormat format) {
        this.format = format;
    }

    public SharedValueDimensions getDimensions() {
        return dimensions;
    }

    public void setDimensions(SharedValueDimensions dimensions) {
        this.dimensions = dimensions;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }
}

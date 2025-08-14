package ai.shreds.shared.value_objects;

import java.util.Objects;

import ai.shreds.shared.enums.SharedEnumImageFormat;

/**
 * Immutable shared image metadata value object as extracted at ingestion time.
 */
public final class SharedValueImageMetadata {
    private final SharedEnumImageFormat format;
    private final SharedValueDimensions dimensions;
    private final long fileSizeBytes;
    private final SharedValueChecksum checksum;

    public SharedValueImageMetadata(SharedEnumImageFormat format,
                                    SharedValueDimensions dimensions,
                                    long fileSizeBytes,
                                    SharedValueChecksum checksum) {
        if (format == null) {
            throw new IllegalArgumentException("format must not be null");
        }
        if (dimensions == null) {
            throw new IllegalArgumentException("dimensions must not be null");
        }
        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("fileSizeBytes must be positive");
        }
        if (checksum == null) {
            throw new IllegalArgumentException("checksum must not be null");
        }
        this.format = format;
        this.dimensions = dimensions;
        this.fileSizeBytes = fileSizeBytes;
        this.checksum = checksum;
    }

    public SharedEnumImageFormat getFormat() {
        return format;
    }

    public SharedValueDimensions getDimensions() {
        return dimensions;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public SharedValueChecksum getChecksum() {
        return checksum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedValueImageMetadata that = (SharedValueImageMetadata) o;
        return fileSizeBytes == that.fileSizeBytes && format == that.format && Objects.equals(dimensions, that.dimensions) && Objects.equals(checksum, that.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, dimensions, fileSizeBytes, checksum);
    }

    @Override
    public String toString() {
        return "SharedValueImageMetadata{" +
                "format=" + format +
                ", dimensions=" + dimensions +
                ", fileSizeBytes=" + fileSizeBytes +
                ", checksum=" + checksum +
                '}';
    }
}

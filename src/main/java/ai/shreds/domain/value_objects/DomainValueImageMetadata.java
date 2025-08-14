package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionValidation;
import ai.shreds.shared.enums.SharedEnumImageFormat;

import java.util.Objects;

/**
 * Immutable value object representing image metadata.
 */
public final class DomainValueImageMetadata {
    private final SharedEnumImageFormat format;
    private final DomainValueDimensions dimensions;
    private final long fileSizeBytes;
    private final DomainValueChecksum checksum;

    private DomainValueImageMetadata(SharedEnumImageFormat format,
                                     DomainValueDimensions dimensions,
                                     long fileSizeBytes,
                                     DomainValueChecksum checksum) {
        if (format == null) {
            throw new DomainExceptionValidation("format must not be null");
        }
        if (dimensions == null) {
            throw new DomainExceptionValidation("dimensions must not be null");
        }
        if (fileSizeBytes <= 0) {
            throw new DomainExceptionValidation("fileSizeBytes must be > 0");
        }
        if (checksum == null) {
            throw new DomainExceptionValidation("checksum must not be null");
        }
        this.format = format;
        this.dimensions = dimensions;
        this.fileSizeBytes = fileSizeBytes;
        this.checksum = checksum;
    }

    public static DomainValueImageMetadata of(SharedEnumImageFormat format,
                                              DomainValueDimensions dimensions,
                                              long fileSizeBytes,
                                              DomainValueChecksum checksum) {
        return new DomainValueImageMetadata(format, dimensions, fileSizeBytes, checksum);
    }

    public SharedEnumImageFormat getFormat() {
        return format;
    }

    public DomainValueDimensions getDimensions() {
        return dimensions;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public DomainValueChecksum getChecksum() {
        return checksum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueImageMetadata)) return false;
        DomainValueImageMetadata that = (DomainValueImageMetadata) o;
        return fileSizeBytes == that.fileSizeBytes &&
                format == that.format &&
                Objects.equals(dimensions, that.dimensions) &&
                Objects.equals(checksum, that.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, dimensions, fileSizeBytes, checksum);
    }

    @Override
    public String toString() {
        return "DomainValueImageMetadata{" +
                "format=" + format +
                ", dimensions=" + dimensions +
                ", fileSizeBytes=" + fileSizeBytes +
                ", checksum=" + checksum +
                '}';
    }
}

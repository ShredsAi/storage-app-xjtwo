package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionValidation;
import java.util.Objects;

/**
 * Value object representing width and height dimensions.
 */
public final class DomainValueDimensions {
    private final int width;
    private final int height;

    private DomainValueDimensions(int width, int height) {
        if (width < 0) {
            throw new DomainExceptionValidation("width must be >= 0");
        }
        if (height < 0) {
            throw new DomainExceptionValidation("height must be >= 0");
        }
        this.width = width;
        this.height = height;
    }

    public static DomainValueDimensions of(int width, int height) {
        return new DomainValueDimensions(width, height);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Returns the aspect ratio (width divided by height).
     */
    public double aspectRatio() {
        if (height == 0) {
            throw new DomainExceptionValidation("height must be > 0 to compute aspect ratio");
        }
        return (double) width / (double) height;
    }

    /**
     * Checks if this dimensions fits within another bounding dimensions.
     */
    public boolean fitsWithin(DomainValueDimensions other) {
        Objects.requireNonNull(other, "other dimensions must not be null");
        return this.width <= other.width && this.height <= other.height;
    }

    /**
     * Scales this dimensions to fit within the given maxWidth and maxHeight, preserving aspect ratio.
     */
    public DomainValueDimensions scaleToFit(int maxWidth, int maxHeight) {
        if (maxWidth < 0 || maxHeight < 0) {
            throw new DomainExceptionValidation("maxWidth and maxHeight must be >= 0");
        }
        if (width == 0 || height == 0) {
            return DomainValueDimensions.of(0, 0);
        }
        double widthRatio = (double) maxWidth / width;
        double heightRatio = (double) maxHeight / height;
        double ratio = Math.min(widthRatio, heightRatio);
        if (ratio >= 1.0) {
            // no scaling needed
            return this;
        }
        int newWidth = (int) Math.floor(width * ratio);
        int newHeight = (int) Math.floor(height * ratio);
        return DomainValueDimensions.of(newWidth, newHeight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueDimensions)) return false;
        DomainValueDimensions that = (DomainValueDimensions) o;
        return width == that.width && height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return "DomainValueDimensions{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}

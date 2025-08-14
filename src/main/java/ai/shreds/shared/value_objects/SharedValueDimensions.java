package ai.shreds.shared.value_objects;

import java.util.Objects;

public final class SharedValueDimensions {
    private final int width;
    private final int height;

    public SharedValueDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double aspectRatio() {
        return (double) width / (double) height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedValueDimensions that = (SharedValueDimensions) o;
        return width == that.width && height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return "SharedValueDimensions{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}

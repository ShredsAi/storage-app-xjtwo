package ai.shreds.shared.value_objects;

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable checksum value object used across layers.
 * Algorithm string should be a canonical uppercase token (e.g., MD5, SHA-1, SHA-256, SHA-512).
 * Value is expected to be a lowercase hex string but we store as-provided; comparisons are case-insensitive.
 */
public final class SharedValueChecksum {
    private final String algorithm;
    private final String value;

    public SharedValueChecksum(String algorithm, String value) {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("algorithm must not be null/blank");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be null/blank");
        }
        this.algorithm = algorithm.trim().toUpperCase(Locale.ROOT);
        this.value = value.trim();
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedValueChecksum that = (SharedValueChecksum) o;
        return algorithm.equals(that.algorithm) && value.equalsIgnoreCase(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(algorithm, value.toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
        return "SharedValueChecksum{" +
                "algorithm='" + algorithm + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}

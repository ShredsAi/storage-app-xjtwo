package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionValidation;
import java.util.Objects;

/**
 * Value object for checksum data.
 */
public final class DomainValueChecksum {
    private final String algorithm;
    private final String value;

    private DomainValueChecksum(String algorithm, String value) {
        if (algorithm == null || algorithm.isBlank()) {
            throw new DomainExceptionValidation("algorithm must be provided");
        }
        if (value == null || value.isBlank()) {
            throw new DomainExceptionValidation("checksum value must be provided");
        }
        this.algorithm = algorithm.trim();
        this.value = value.trim();
    }

    public static DomainValueChecksum of(String algorithm, String value) {
        return new DomainValueChecksum(algorithm, value);
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getValue() {
        return value;
    }

    /**
     * Returns a normalized algorithm name suitable for MessageDigest, e.g., SHA_256 -> SHA-256.
     */
    public String normalizedAlgorithm() {
        return algorithm.replace('_', '-').toUpperCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValueChecksum)) return false;
        DomainValueChecksum that = (DomainValueChecksum) o;
        return Objects.equals(algorithm, that.algorithm) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(algorithm, value);
    }

    @Override
    public String toString() {
        return "DomainValueChecksum{" +
                "algorithm='" + algorithm + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}

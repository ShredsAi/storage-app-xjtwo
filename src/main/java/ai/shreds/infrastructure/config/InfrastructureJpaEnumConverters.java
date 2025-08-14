package ai.shreds.infrastructure.config;

import ai.shreds.shared.enums.SharedEnumChecksumAlgorithm;
import ai.shreds.shared.enums.SharedEnumImageFormat;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumImageStatus;
import ai.shreds.shared.enums.SharedEnumProcessingStatus;
import ai.shreds.shared.enums.SharedEnumVariantType;
import org.springframework.stereotype.Component;

@Component
public class InfrastructureJpaEnumConverters {

    public String toDbImageStatus(SharedEnumImageStatus status) {
        return status == null ? null : status.name();
    }

    public SharedEnumImageStatus fromDbImageStatus(String value) {
        return value == null ? null : SharedEnumImageStatus.valueOf(value);
    }

    public String toDbImageRole(SharedEnumImageRole role) {
        return role == null ? null : role.name();
    }

    public SharedEnumImageRole fromDbImageRole(String value) {
        return value == null ? null : SharedEnumImageRole.valueOf(value);
    }

    public String toDbImageFormat(SharedEnumImageFormat fmt) {
        return fmt == null ? null : fmt.name();
    }

    public SharedEnumImageFormat fromDbImageFormat(String value) {
        return value == null ? null : SharedEnumImageFormat.valueOf(value);
    }

    public String toDbVariantType(SharedEnumVariantType t) {
        return t == null ? null : t.name();
    }

    public SharedEnumVariantType fromDbVariantType(String value) {
        return value == null ? null : SharedEnumVariantType.valueOf(value);
    }

    public String toDbProcessingStatus(SharedEnumProcessingStatus status) {
        return status == null ? null : status.name();
    }

    public SharedEnumProcessingStatus fromDbProcessingStatus(String value) {
        return value == null ? null : SharedEnumProcessingStatus.valueOf(value);
    }

    public String toDbChecksumAlgorithm(SharedEnumChecksumAlgorithm algo) {
        if (algo == null) return null;
        switch (algo) {
            case MD5: return "MD5";
            case SHA_1: return "SHA-1";
            case SHA_256: return "SHA-256";
            case SHA_512: return "SHA-512";
            default: return algo.name();
        }
    }

    public SharedEnumChecksumAlgorithm fromDbChecksumAlgorithm(String value) {
        if (value == null) return null;
        String normalized = value.replace('-', '_');
        switch (normalized) {
            case "MD5": return SharedEnumChecksumAlgorithm.MD5;
            case "SHA_1": return SharedEnumChecksumAlgorithm.SHA_1;
            case "SHA_256": return SharedEnumChecksumAlgorithm.SHA_256;
            case "SHA_512": return SharedEnumChecksumAlgorithm.SHA_512;
            default: return SharedEnumChecksumAlgorithm.valueOf(normalized);
        }
    }
}

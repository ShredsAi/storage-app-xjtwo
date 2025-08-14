package ai.shreds.shared.dtos;

import ai.shreds.shared.enums.SharedEnumChecksumAlgorithm;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.exceptions.SharedExceptionValidation;

/**
 * Upload payload DTO. All fields are optional; application layer may apply defaults.
 */
public class SharedDTOImageUploadPayload {

    private String title;
    private String altText;
    private SharedEnumImageRole role;
    private SharedEnumChecksumAlgorithm checksumAlgorithm;
    private String idempotencyKey;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public SharedEnumImageRole getRole() {
        return role;
    }

    public void setRole(SharedEnumImageRole role) {
        this.role = role;
    }

    public SharedEnumChecksumAlgorithm getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    public void setChecksumAlgorithm(SharedEnumChecksumAlgorithm checksumAlgorithm) {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public static void validate(SharedDTOImageUploadPayload payload) {
        if (payload == null) {
            throw new SharedExceptionValidation("Image upload payload must not be null");
        }
        if (payload.title != null && payload.title.length() > 256) {
            throw new SharedExceptionValidation("title must be at most 256 characters");
        }
        if (payload.altText != null && payload.altText.length() > 512) {
            throw new SharedExceptionValidation("altText must be at most 512 characters");
        }
        if (payload.idempotencyKey != null && payload.idempotencyKey.length() > 64) {
            throw new SharedExceptionValidation("idempotencyKey must be at most 64 characters");
        }
        // role and checksumAlgorithm are optional; application layer may default them if null
    }

    /** Instance-level convenience that aligns with UML {static}+validate(): allow calling payload.validate(). */
    public void validate() {
        validate(this);
    }
}

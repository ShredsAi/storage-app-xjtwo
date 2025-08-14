package ai.shreds.shared.dtos;

import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.exceptions.SharedExceptionValidation;

public class SharedDTOImageUpdateRequest {
    private String title;
    private String altText;
    private SharedEnumImageRole role;

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

    public static void validate(SharedDTOImageUpdateRequest request) {
        if (request == null) {
            throw new SharedExceptionValidation("Image update request must not be null");
        }
        if (request.title == null && request.altText == null && request.role == null) {
            throw new SharedExceptionValidation("At least one updatable field (title, altText, role) must be provided");
        }
        if (request.title != null && request.title.length() > 256) {
            throw new SharedExceptionValidation("title must be at most 256 characters");
        }
        if (request.altText != null && request.altText.length() > 512) {
            throw new SharedExceptionValidation("altText must be at most 512 characters");
        }
    }

    public void validate() {
        validate(this);
    }
}

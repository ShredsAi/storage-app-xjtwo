package ai.shreds.shared.dtos;

import java.time.Instant;
import java.util.UUID;

import ai.shreds.shared.enums.SharedEnumImageStatus;

public class SharedDTOImageStatusResponse {
    private UUID imageId;
    private SharedEnumImageStatus status;
    private Instant updatedAt;

    public SharedDTOImageStatusResponse() {
    }

    public SharedDTOImageStatusResponse(UUID imageId, SharedEnumImageStatus status, Instant updatedAt) {
        this.imageId = imageId;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public UUID getImageId() {
        return imageId;
    }

    public void setImageId(UUID imageId) {
        this.imageId = imageId;
    }

    public SharedEnumImageStatus getStatus() {
        return status;
    }

    public void setStatus(SharedEnumImageStatus status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SharedDTOImageStatusResponse{" +
                "imageId=" + imageId +
                ", status=" + status +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

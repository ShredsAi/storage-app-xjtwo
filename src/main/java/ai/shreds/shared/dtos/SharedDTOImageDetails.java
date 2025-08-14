package ai.shreds.shared.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumImageStatus;
import ai.shreds.shared.value_objects.SharedValueImageMetadata;

public class SharedDTOImageDetails {
    private UUID imageId;
    private UUID itemId;
    private SharedEnumImageRole role;
    private String title;
    private String altText;
    private SharedEnumImageStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private SharedValueImageMetadata metadata;
    private List<SharedDTOImageVariant> variants;

    public UUID getImageId() {
        return imageId;
    }

    public void setImageId(UUID imageId) {
        this.imageId = imageId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public SharedEnumImageRole getRole() {
        return role;
    }

    public void setRole(SharedEnumImageRole role) {
        this.role = role;
    }

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

    public SharedEnumImageStatus getStatus() {
        return status;
    }

    public void setStatus(SharedEnumImageStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public SharedValueImageMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(SharedValueImageMetadata metadata) {
        this.metadata = metadata;
    }

    public List<SharedDTOImageVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<SharedDTOImageVariant> variants) {
        this.variants = variants;
    }

    @Override
    public String toString() {
        return "SharedDTOImageDetails{" +
                "imageId=" + imageId +
                ", itemId=" + itemId +
                ", role=" + role +
                ", title='" + title + '\'' +
                ", altText='" + altText + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", metadata=" + metadata +
                ", variants=" + variants +
                '}';
    }
}

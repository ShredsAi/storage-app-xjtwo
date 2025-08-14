package ai.shreds.shared.dtos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import ai.shreds.shared.enums.SharedEnumVariantType;

public class SharedDTOReprocessResponse {
    private UUID imageId;
    private Set<SharedEnumVariantType> requestedVariants;
    private List<SharedDTOProcessingTaskSummary> tasks;
    private List<SharedDTOImageVariant> variants;

    public SharedDTOReprocessResponse() {
    }

    public SharedDTOReprocessResponse(UUID imageId,
                                      Set<SharedEnumVariantType> requestedVariants,
                                      List<SharedDTOProcessingTaskSummary> tasks,
                                      List<SharedDTOImageVariant> variants) {
        this.imageId = imageId;
        this.requestedVariants = requestedVariants;
        this.tasks = tasks;
        this.variants = variants;
    }

    public UUID getImageId() {
        return imageId;
    }

    public void setImageId(UUID imageId) {
        this.imageId = imageId;
    }

    public Set<SharedEnumVariantType> getRequestedVariants() {
        return requestedVariants;
    }

    public void setRequestedVariants(Set<SharedEnumVariantType> requestedVariants) {
        this.requestedVariants = requestedVariants;
    }

    public List<SharedDTOProcessingTaskSummary> getTasks() {
        return tasks;
    }

    public void setTasks(List<SharedDTOProcessingTaskSummary> tasks) {
        this.tasks = tasks;
    }

    public List<SharedDTOImageVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<SharedDTOImageVariant> variants) {
        this.variants = variants;
    }

    @Override
    public String toString() {
        return "SharedDTOReprocessResponse{" +
                "imageId=" + imageId +
                ", requestedVariants=" + requestedVariants +
                ", tasks=" + tasks +
                ", variants=" + variants +
                '}';
    }
}

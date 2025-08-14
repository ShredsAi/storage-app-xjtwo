package ai.shreds.application.dtos;

import java.util.UUID;
import ai.shreds.shared.dtos.SharedDTOReprocessRequest;
import java.util.Objects;

public class ApplicationDTOReprocessVariantsCommand {
    private final UUID imageId;
    private final SharedDTOReprocessRequest request;

    public ApplicationDTOReprocessVariantsCommand(UUID imageId, SharedDTOReprocessRequest request) {
        this.imageId = Objects.requireNonNull(imageId, "imageId must not be null");
        this.request = Objects.requireNonNull(request, "request must not be null");
    }

    public UUID getImageId() {
        return imageId;
    }

    public SharedDTOReprocessRequest getRequest() {
        return request;
    }

    /**
     * Returns a string representation of arguments for logging or tracing.
     */
    public String toArgs() {
        return String.format("ApplicationDTOReprocessVariantsCommand[imageId=%s, requestedVariants=%s]",
                imageId, request.getRequestedVariants());
    }
}
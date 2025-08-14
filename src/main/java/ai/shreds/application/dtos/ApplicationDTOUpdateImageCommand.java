package ai.shreds.application.dtos;

import java.util.Objects;
import java.util.UUID;
import ai.shreds.shared.dtos.SharedDTOImageUpdateRequest;

public class ApplicationDTOUpdateImageCommand {
    private final UUID imageId;
    private final SharedDTOImageUpdateRequest request;

    public ApplicationDTOUpdateImageCommand(UUID imageId, SharedDTOImageUpdateRequest request) {
        this.imageId = Objects.requireNonNull(imageId, "imageId must not be null");
        this.request = Objects.requireNonNull(request, "request must not be null");
    }

    public UUID getImageId() {
        return imageId;
    }

    public SharedDTOImageUpdateRequest getRequest() {
        return request;
    }

    public String toArgs() {
        return String.format(
            "ApplicationDTOUpdateImageCommand[imageId=%s, title=%s, altText=%s, role=%s]",
            imageId,
            request.getTitle(),
            request.getAltText(),
            request.getRole()
        );
    }
}

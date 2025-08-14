package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedDTOImageDetails;
import ai.shreds.shared.dtos.SharedDTOImageUpdateRequest;
import java.util.UUID;

public interface ApplicationInputPortUpdateImageMetadata {
    SharedDTOImageDetails updateImage(UUID imageId, SharedDTOImageUpdateRequest request);
}

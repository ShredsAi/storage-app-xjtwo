package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedDTOImageDetails;
import java.util.UUID;

public interface ApplicationInputPortGetImage {
    SharedDTOImageDetails getImage(UUID imageId);
}

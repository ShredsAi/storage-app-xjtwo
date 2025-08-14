package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedDTOImageStatusResponse;
import java.util.UUID;

public interface ApplicationInputPortActivateImage {
    SharedDTOImageStatusResponse activate(UUID imageId);
}

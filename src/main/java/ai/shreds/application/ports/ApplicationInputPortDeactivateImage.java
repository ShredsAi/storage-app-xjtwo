package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedDTOImageStatusResponse;
import java.util.UUID;

public interface ApplicationInputPortDeactivateImage {

    /**
     * Deactivate an image by its ID, transitioning status to INACTIVE.
     *
     * @param imageId the UUID of the image to deactivate
     * @return DTO containing the image ID, new status, and updated timestamp
     */
    SharedDTOImageStatusResponse deactivate(UUID imageId);
}
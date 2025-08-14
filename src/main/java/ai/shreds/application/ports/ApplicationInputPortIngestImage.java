package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedDTOImageDetails;
import ai.shreds.shared.dtos.SharedDTOImageUploadPayload;
import ai.shreds.shared.value_objects.SharedValueUploadFile;
import java.util.UUID;

public interface ApplicationInputPortIngestImage {
    SharedDTOImageDetails ingestImage(UUID itemId, SharedValueUploadFile file, SharedDTOImageUploadPayload payload, String idempotencyKey);
}

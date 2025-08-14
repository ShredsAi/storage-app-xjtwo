package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedDTOPaginatedImages;
import ai.shreds.shared.value_objects.SharedValueListQueryParams;
import java.util.UUID;

public interface ApplicationInputPortListImages {
    SharedDTOPaginatedImages listImages(UUID itemId, SharedValueListQueryParams params);
}

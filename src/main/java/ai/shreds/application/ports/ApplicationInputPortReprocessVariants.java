package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedDTOReprocessRequest;
import ai.shreds.shared.dtos.SharedDTOReprocessResponse;
import java.util.UUID;

public interface ApplicationInputPortReprocessVariants {
    SharedDTOReprocessResponse reprocess(UUID imageId, SharedDTOReprocessRequest request);
}

package ai.shreds.adapters.primary.mappers;

import ai.shreds.application.dtos.ApplicationDTOIngestImageCommand;
import ai.shreds.application.dtos.ApplicationDTOListImagesQuery;
import ai.shreds.application.dtos.ApplicationDTOReprocessVariantsCommand;
import ai.shreds.application.dtos.ApplicationDTOUpdateImageCommand;
import ai.shreds.shared.value_objects.SharedValueUploadFile;
import ai.shreds.shared.value_objects.SharedValueListQueryParams;
import ai.shreds.shared.dtos.SharedDTOImageUploadPayload;
import ai.shreds.shared.dtos.SharedDTOImageUpdateRequest;
import ai.shreds.shared.dtos.SharedDTOReprocessRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Component
public class AdapterImageApiMapper {

    public ApplicationDTOIngestImageCommand toIngestCommand(
            UUID itemId,
            SharedValueUploadFile file,
            SharedDTOImageUploadPayload payload,
            String idempotencyKey
    ) {
        return new ApplicationDTOIngestImageCommand(itemId, file, payload, idempotencyKey);
    }

    public ApplicationDTOListImagesQuery toListQuery(
            UUID itemId,
            SharedValueListQueryParams params
    ) {
        return new ApplicationDTOListImagesQuery(itemId, params);
    }

    public ApplicationDTOUpdateImageCommand toUpdateCommand(
            UUID imageId,
            SharedDTOImageUpdateRequest request
    ) {
        return new ApplicationDTOUpdateImageCommand(imageId, request);
    }

    public ApplicationDTOReprocessVariantsCommand toReprocessCommand(
            UUID imageId,
            SharedDTOReprocessRequest request
    ) {
        return new ApplicationDTOReprocessVariantsCommand(imageId, request);
    }

    public SharedValueUploadFile toSharedValueUploadFile(MultipartFile multipart) throws IOException {
        return new SharedValueUploadFile(
                multipart.getOriginalFilename(),
                multipart.getContentType(),
                multipart.getSize(),
                multipart.getBytes()
        );
    }
}

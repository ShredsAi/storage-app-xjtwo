package ai.shreds.application.dtos;

import java.util.Objects;
import java.util.UUID;
import ai.shreds.shared.value_objects.SharedValueUploadFile;
import ai.shreds.shared.dtos.SharedDTOImageUploadPayload;

public class ApplicationDTOIngestImageCommand {
    private final UUID itemId;
    private final SharedValueUploadFile file;
    private final SharedDTOImageUploadPayload payload;
    private final String idempotencyKey;

    public ApplicationDTOIngestImageCommand(UUID itemId, SharedValueUploadFile file, SharedDTOImageUploadPayload payload, String idempotencyKey) {
        this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        this.file = Objects.requireNonNull(file, "file must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.idempotencyKey = idempotencyKey; // can be null if header not provided
    }

    public UUID getItemId() {
        return itemId;
    }

    public SharedValueUploadFile getFile() {
        return file;
    }

    public SharedDTOImageUploadPayload getPayload() {
        return payload;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String toArgs() {
        return String.format(
            "ApplicationDTOIngestImageCommand[itemId=%s, fileName=%s, contentType=%s, contentLength=%d, role=%s, checksumAlgo=%s, idemKey=%s]",
            itemId,
            file.getOriginalFileName(),
            file.getContentType(),
            file.getContentLength(),
            payload.getRole(),
            payload.getChecksumAlgorithm(),
            idempotencyKey
        );
    }
}

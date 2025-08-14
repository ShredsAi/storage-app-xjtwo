package ai.shreds.application.dtos;

import java.util.UUID;
import ai.shreds.shared.value_objects.SharedValueListQueryParams;
import java.util.Objects;

public class ApplicationDTOListImagesQuery {

    private final UUID itemId;
    private final SharedValueListQueryParams params;

    public ApplicationDTOListImagesQuery(UUID itemId, SharedValueListQueryParams params) {
        this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        this.params = Objects.requireNonNull(params, "params must not be null");
    }

    public UUID getItemId() {
        return itemId;
    }

    public SharedValueListQueryParams getParams() {
        return params;
    }

    /**
     * Returns a string representation of arguments for logging or tracing.
     */
    public String toArgs() {
        return String.format(
            "ApplicationDTOListImagesQuery[itemId=%s, status=%s, role=%s, page=%d, size=%d, sort=%s]",
            itemId,
            params.getStatus(),
            params.getRole(),
            params.getPage(),
            params.getSize(),
            params.getSort()
        );
    }
}
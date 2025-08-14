package ai.shreds.domain.ports;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for idempotency key lookup/storage.
 */
public interface DomainOutputPortIdempotencyRepository {
    /**
     * Returns the imageId previously associated with the given idempotencyKey, if any.
     */
    Optional<UUID> findImageIdByKey(String idempotencyKey);

    /**
     * Records an idempotency key mapping to an imageId with an expiration timestamp.
     */
    void recordKey(String idempotencyKey, UUID imageId, Instant expiresAt);
}

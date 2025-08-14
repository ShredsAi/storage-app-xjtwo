package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityImageVariant;

import java.util.Set;
import java.util.UUID;

/**
 * Output port for persisting and loading image variants.
 */
public interface DomainOutputPortImageVariantRepository {
    void saveAll(UUID imageId, Set<DomainEntityImageVariant> variants);

    Set<DomainEntityImageVariant> findByImageId(UUID imageId);
}

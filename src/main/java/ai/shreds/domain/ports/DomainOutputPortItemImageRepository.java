package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainAggregateItemImage;
import ai.shreds.domain.value_objects.DomainValuePage;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumImageStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for persisting and querying ItemImage aggregates.
 */
public interface DomainOutputPortItemImageRepository {
    DomainAggregateItemImage save(DomainAggregateItemImage aggregate);

    Optional<DomainAggregateItemImage> findById(UUID imageId);

    DomainValuePage<DomainAggregateItemImage> listByItemIdWithFilters(
            UUID itemId,
            SharedEnumImageStatus statusFilter,
            SharedEnumImageRole roleFilter,
            int page,
            int size,
            String sort
    );
}

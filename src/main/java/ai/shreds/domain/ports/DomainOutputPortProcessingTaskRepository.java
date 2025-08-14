package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainEntityImageProcessingTask;

import java.util.Set;
import java.util.UUID;

/**
 * Output port for image processing task persistence and queries.
 */
public interface DomainOutputPortProcessingTaskRepository {
    DomainEntityImageProcessingTask create(DomainEntityImageProcessingTask task);

    Set<DomainEntityImageProcessingTask> findRunningByImageId(UUID imageId);
}

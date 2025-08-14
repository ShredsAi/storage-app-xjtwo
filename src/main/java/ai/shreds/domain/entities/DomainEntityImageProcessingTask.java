package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import ai.shreds.domain.exceptions.DomainExceptionValidation;
import ai.shreds.shared.enums.SharedEnumProcessingStatus;
import ai.shreds.shared.enums.SharedEnumVariantType;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Image processing task entity representing queued/ongoing/completed work for variant generation.
 */
public class DomainEntityImageProcessingTask {
    private final UUID taskId;
    private final UUID imageId;
    private SharedEnumProcessingStatus status;
    private final Instant requestedAt;
    private Instant completedAt;
    private final Set<SharedEnumVariantType> requestedVariants;

    public DomainEntityImageProcessingTask(UUID taskId,
                                           UUID imageId,
                                           SharedEnumProcessingStatus status,
                                           Instant requestedAt,
                                           Instant completedAt,
                                           Set<SharedEnumVariantType> requestedVariants) {
        if (taskId == null) throw new DomainExceptionValidation("taskId must not be null");
        if (imageId == null) throw new DomainExceptionValidation("imageId must not be null");
        if (status == null) throw new DomainExceptionValidation("status must not be null");
        if (requestedAt == null) throw new DomainExceptionValidation("requestedAt must not be null");
        if (requestedVariants == null || requestedVariants.isEmpty()) {
            throw new DomainExceptionValidation("requestedVariants must be non-empty");
        }
        this.taskId = taskId;
        this.imageId = imageId;
        this.status = status;
        this.requestedAt = requestedAt;
        this.completedAt = completedAt;
        this.requestedVariants = Collections.unmodifiableSet(EnumSet.copyOf(requestedVariants));
        // Temporal invariants
        if (isTerminal(status)) {
            if (completedAt == null) throw new DomainExceptionValidation("completedAt must be set when status is COMPLETED or FAILED");
            if (completedAt.isBefore(requestedAt)) throw new DomainExceptionValidation("completedAt must be >= requestedAt");
        } else {
            if (completedAt != null) throw new DomainExceptionValidation("completedAt must be null unless status is COMPLETED or FAILED");
        }
    }

    public static DomainEntityImageProcessingTask pending(UUID imageId, Set<SharedEnumVariantType> requestedVariants, Instant now) {
        if (now == null) now = Instant.now();
        return new DomainEntityImageProcessingTask(UUID.randomUUID(), imageId, SharedEnumProcessingStatus.PENDING, now, null, requestedVariants);
    }

    private static boolean isTerminal(SharedEnumProcessingStatus s) {
        return s == SharedEnumProcessingStatus.COMPLETED || s == SharedEnumProcessingStatus.FAILED;
    }

    public UUID getTaskId() { return taskId; }
    public UUID getImageId() { return imageId; }
    public SharedEnumProcessingStatus getStatus() { return status; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Set<SharedEnumVariantType> toRequestedVariants() { return requestedVariants; }

    public void start() {
        if (status != SharedEnumProcessingStatus.PENDING) {
            throw new DomainExceptionInvariantViolation("Only PENDING tasks can be started");
        }
        this.status = SharedEnumProcessingStatus.RUNNING;
    }

    public void completeSuccessfully(Instant completedAt) {
        if (status != SharedEnumProcessingStatus.RUNNING) {
            throw new DomainExceptionInvariantViolation("Only RUNNING tasks can be completed");
        }
        if (completedAt == null) throw new DomainExceptionValidation("completedAt must not be null");
        if (completedAt.isBefore(this.requestedAt)) throw new DomainExceptionValidation("completedAt must be >= requestedAt");
        this.status = SharedEnumProcessingStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public void fail(Instant completedAt) {
        if (status != SharedEnumProcessingStatus.RUNNING) {
            throw new DomainExceptionInvariantViolation("Only RUNNING tasks can fail");
        }
        if (completedAt == null) throw new DomainExceptionValidation("completedAt must not be null");
        if (completedAt.isBefore(this.requestedAt)) throw new DomainExceptionValidation("completedAt must be >= requestedAt");
        this.status = SharedEnumProcessingStatus.FAILED;
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainEntityImageProcessingTask)) return false;
        DomainEntityImageProcessingTask that = (DomainEntityImageProcessingTask) o;
        return Objects.equals(taskId, that.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }
}

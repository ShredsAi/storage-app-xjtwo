package ai.shreds.shared.dtos;

import java.time.Instant;
import java.util.UUID;

import ai.shreds.shared.enums.SharedEnumProcessingStatus;

public class SharedDTOProcessingTaskSummary {
    private UUID taskId;
    private SharedEnumProcessingStatus status;
    private Instant requestedAt;
    private Instant completedAt;

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public SharedEnumProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(SharedEnumProcessingStatus status) {
        this.status = status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "SharedDTOProcessingTaskSummary{" +
                "taskId=" + taskId +
                ", status=" + status +
                ", requestedAt=" + requestedAt +
                ", completedAt=" + completedAt +
                '}';
    }
}

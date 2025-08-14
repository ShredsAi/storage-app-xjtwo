package ai.shreds.infrastructure.repositories.jpa.entities;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "task_requested_variant")
public class InfrastructureJpaTaskRequestedVariantEntity {

    @EmbeddedId
    private InfrastructureJpaTaskRequestedVariantId id;

    public InfrastructureJpaTaskRequestedVariantEntity() {}

    public InfrastructureJpaTaskRequestedVariantEntity(UUID taskId, String variantType) {
        this.id = new InfrastructureJpaTaskRequestedVariantId(taskId, variantType);
    }

    public InfrastructureJpaTaskRequestedVariantId getId() { return id; }
    public void setId(InfrastructureJpaTaskRequestedVariantId id) { this.id = id; }

    @Transient
    public UUID getTaskId() { return id != null ? id.getTaskId() : null; }

    @Transient
    public String getVariantType() { return id != null ? id.getVariantType() : null; }

    @Embeddable
    public static class InfrastructureJpaTaskRequestedVariantId implements Serializable {
        @Column(name = "task_id", nullable = false)
        private UUID taskId;

        @Column(name = "variant_type", nullable = false, length = 20)
        private String variantType;

        public InfrastructureJpaTaskRequestedVariantId() {}

        public InfrastructureJpaTaskRequestedVariantId(UUID taskId, String variantType) {
            this.taskId = taskId;
            this.variantType = variantType;
        }

        public UUID getTaskId() { return taskId; }
        public void setTaskId(UUID taskId) { this.taskId = taskId; }

        public String getVariantType() { return variantType; }
        public void setVariantType(String variantType) { this.variantType = variantType; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InfrastructureJpaTaskRequestedVariantId)) return false;
            InfrastructureJpaTaskRequestedVariantId that = (InfrastructureJpaTaskRequestedVariantId) o;
            return Objects.equals(taskId, that.taskId) && Objects.equals(variantType, that.variantType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(taskId, variantType);
        }
    }
}

package ai.shreds.infrastructure.repositories.jpa.entities;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_record")
public class InfrastructureJpaIdempotencyRecordEntity {

    @Id
    @Column(name = "idempotency_key", length = 64, nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "image_id", nullable = false)
    private UUID imageId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public InfrastructureJpaIdempotencyRecordEntity() {}

    public InfrastructureJpaIdempotencyRecordEntity(String idempotencyKey, UUID imageId, Instant createdAt, Instant expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.imageId = imageId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public UUID getImageId() { return imageId; }
    public void setImageId(UUID imageId) { this.imageId = imageId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}

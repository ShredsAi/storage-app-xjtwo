package ai.shreds.infrastructure.repositories.impl;

import ai.shreds.domain.ports.DomainOutputPortIdempotencyRepository;
import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaIdempotencyRecordEntity;
import ai.shreds.infrastructure.repositories.jpa.spring.InfrastructureJpaIdempotencyRecordRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InfrastructureRepositoryImplIdempotencyJpa implements DomainOutputPortIdempotencyRepository {

    private final InfrastructureJpaIdempotencyRecordRepository jpaIdemRepo;

    public InfrastructureRepositoryImplIdempotencyJpa(InfrastructureJpaIdempotencyRecordRepository jpaIdemRepo) {
        this.jpaIdemRepo = jpaIdemRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findImageIdByKey(String idempotencyKey) {
        return jpaIdemRepo.findById(idempotencyKey).map(InfrastructureJpaIdempotencyRecordEntity::getImageId);
    }

    @Override
    @Transactional
    public void recordKey(String idempotencyKey, UUID imageId, Instant expiresAt) {
        InfrastructureJpaIdempotencyRecordEntity entity = new InfrastructureJpaIdempotencyRecordEntity(
                idempotencyKey, imageId, Instant.now(), expiresAt
        );
        jpaIdemRepo.save(entity);
    }
}

package ai.shreds.infrastructure.repositories.jpa.spring;

import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaIdempotencyRecordEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InfrastructureJpaIdempotencyRecordRepository extends JpaRepository<InfrastructureJpaIdempotencyRecordEntity, String> {
    @Override
    Optional<InfrastructureJpaIdempotencyRecordEntity> findById(String idempotencyKey);
}

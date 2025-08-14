package ai.shreds.infrastructure.repositories.jpa.spring;

import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaImageVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InfrastructureJpaImageVariantRepository extends JpaRepository<InfrastructureJpaImageVariantEntity, UUID> {

    List<InfrastructureJpaImageVariantEntity> findByImageId(UUID imageId);
}

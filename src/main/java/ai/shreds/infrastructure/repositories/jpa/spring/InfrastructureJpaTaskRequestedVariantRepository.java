package ai.shreds.infrastructure.repositories.jpa.spring;

import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaTaskRequestedVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InfrastructureJpaTaskRequestedVariantRepository extends JpaRepository<InfrastructureJpaTaskRequestedVariantEntity, InfrastructureJpaTaskRequestedVariantEntity.InfrastructureJpaTaskRequestedVariantId> {

    @Query("select t from InfrastructureJpaTaskRequestedVariantEntity t where t.id.taskId = :taskId")
    List<InfrastructureJpaTaskRequestedVariantEntity> findByTaskId(@Param("taskId") UUID taskId);
}

package ai.shreds.infrastructure.repositories.jpa.spring;

import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaImageProcessingTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InfrastructureJpaImageProcessingTaskRepository extends JpaRepository<InfrastructureJpaImageProcessingTaskEntity, UUID> {

    @Query("select t from InfrastructureJpaImageProcessingTaskEntity t where t.imageId = :imageId and t.status = 'RUNNING'")
    List<InfrastructureJpaImageProcessingTaskEntity> findRunningByImageId(@Param("imageId") UUID imageId);

    @Query("select t from InfrastructureJpaImageProcessingTaskEntity t where t.imageId = :imageId")
    List<InfrastructureJpaImageProcessingTaskEntity> findByImageId(@Param("imageId") UUID imageId);
}

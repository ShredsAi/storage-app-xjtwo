package ai.shreds.infrastructure.repositories.jpa.spring;

import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaItemImageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InfrastructureJpaItemImageRepository extends JpaRepository<InfrastructureJpaItemImageEntity, UUID> {

    @Query("select i from InfrastructureJpaItemImageEntity i " +
           "where i.itemId = :itemId " +
           "and (:status is null or :status = '' or i.status = :status) " +
           "and (:role is null or :role = '' or i.role = :role)")
    Page<InfrastructureJpaItemImageEntity> findAllByItemIdAndFilters(@Param("itemId") UUID itemId,
                                                                     @Param("status") String status,
                                                                     @Param("role") String role,
                                                                     Pageable pageable);
}

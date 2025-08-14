package ai.shreds.infrastructure.repositories.impl;

import ai.shreds.domain.entities.DomainEntityImageProcessingTask;
import ai.shreds.domain.ports.DomainOutputPortProcessingTaskRepository;
import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaImageProcessingTaskEntity;
import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaTaskRequestedVariantEntity;
import ai.shreds.infrastructure.repositories.jpa.spring.InfrastructureJpaImageProcessingTaskRepository;
import ai.shreds.infrastructure.repositories.jpa.spring.InfrastructureJpaTaskRequestedVariantRepository;
import ai.shreds.infrastructure.repositories.mappers.InfrastructureMapperJpaToDomain;
import ai.shreds.shared.enums.SharedEnumVariantType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class InfrastructureRepositoryImplProcessingTaskJpa implements DomainOutputPortProcessingTaskRepository {

    private final InfrastructureJpaImageProcessingTaskRepository jpaTaskRepo;
    private final InfrastructureJpaTaskRequestedVariantRepository jpaTaskReqRepo;
    private final InfrastructureMapperJpaToDomain mapper;

    public InfrastructureRepositoryImplProcessingTaskJpa(InfrastructureJpaImageProcessingTaskRepository jpaTaskRepo,
                                                         InfrastructureJpaTaskRequestedVariantRepository jpaTaskReqRepo,
                                                         InfrastructureMapperJpaToDomain mapper) {
        this.jpaTaskRepo = jpaTaskRepo;
        this.jpaTaskReqRepo = jpaTaskReqRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public DomainEntityImageProcessingTask create(DomainEntityImageProcessingTask task) {
        InfrastructureJpaImageProcessingTaskEntity saved = jpaTaskRepo.save(mapper.toJpaTask(task));
        Set<SharedEnumVariantType> req = task.toRequestedVariants();
        if (req != null && !req.isEmpty()) {
            List<InfrastructureJpaTaskRequestedVariantEntity> requested = mapper.toJpaRequestedVariants(task.getTaskId(), req);
            jpaTaskReqRepo.saveAll(requested);
        }
        List<InfrastructureJpaTaskRequestedVariantEntity> reqSaved = jpaTaskReqRepo.findByTaskId(saved.getTaskId());
        Set<SharedEnumVariantType> requestedTypes = reqSaved.stream()
                .map(e -> SharedEnumVariantType.valueOf(e.getId().getVariantType()))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SharedEnumVariantType.class)));
        return mapper.toDomainTask(saved, requestedTypes);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<DomainEntityImageProcessingTask> findRunningByImageId(UUID imageId) {
        List<InfrastructureJpaImageProcessingTaskEntity> running = jpaTaskRepo.findRunningByImageId(imageId);
        if (running.isEmpty()) return Collections.emptySet();
        return running.stream().map(t -> {
            List<InfrastructureJpaTaskRequestedVariantEntity> req = jpaTaskReqRepo.findByTaskId(t.getTaskId());
            Set<SharedEnumVariantType> types = req.stream()
                    .map(e -> SharedEnumVariantType.valueOf(e.getId().getVariantType()))
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(SharedEnumVariantType.class)));
            return mapper.toDomainTask(t, types);
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

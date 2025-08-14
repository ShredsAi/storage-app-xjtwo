package ai.shreds.infrastructure.repositories.impl;

import ai.shreds.domain.entities.DomainAggregateItemImage;
import ai.shreds.domain.entities.DomainEntityImageProcessingTask;
import ai.shreds.domain.entities.DomainEntityImageVariant;
import ai.shreds.domain.ports.DomainOutputPortItemImageRepository;
import ai.shreds.domain.value_objects.DomainValuePage;
import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaImageProcessingTaskEntity;
import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaImageVariantEntity;
import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaItemImageEntity;
import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaTaskRequestedVariantEntity;
import ai.shreds.infrastructure.repositories.jpa.spring.InfrastructureJpaImageProcessingTaskRepository;
import ai.shreds.infrastructure.repositories.jpa.spring.InfrastructureJpaImageVariantRepository;
import ai.shreds.infrastructure.repositories.jpa.spring.InfrastructureJpaItemImageRepository;
import ai.shreds.infrastructure.repositories.jpa.spring.InfrastructureJpaTaskRequestedVariantRepository;
import ai.shreds.infrastructure.repositories.mappers.InfrastructureMapperJpaToDomain;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumImageStatus;
import ai.shreds.shared.enums.SharedEnumVariantType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class InfrastructureRepositoryImplItemImageJpa implements DomainOutputPortItemImageRepository {

    private final InfrastructureJpaItemImageRepository jpaItemRepo;
    private final InfrastructureJpaImageVariantRepository jpaVariantRepo;
    private final InfrastructureJpaImageProcessingTaskRepository jpaTaskRepo;
    private final InfrastructureJpaTaskRequestedVariantRepository jpaTaskReqRepo;
    private final InfrastructureMapperJpaToDomain mapper;

    public InfrastructureRepositoryImplItemImageJpa(InfrastructureJpaItemImageRepository jpaItemRepo,
                                                    InfrastructureJpaImageVariantRepository jpaVariantRepo,
                                                    InfrastructureJpaImageProcessingTaskRepository jpaTaskRepo,
                                                    InfrastructureJpaTaskRequestedVariantRepository jpaTaskReqRepo,
                                                    InfrastructureMapperJpaToDomain mapper) {
        this.jpaItemRepo = jpaItemRepo;
        this.jpaVariantRepo = jpaVariantRepo;
        this.jpaTaskRepo = jpaTaskRepo;
        this.jpaTaskReqRepo = jpaTaskReqRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public DomainAggregateItemImage save(DomainAggregateItemImage aggregate) {
        // Save root
        InfrastructureJpaItemImageEntity saved = jpaItemRepo.save(mapper.toJpaItemImage(aggregate));
        // Save variants (upsert semantics)
        List<InfrastructureJpaImageVariantEntity> vEntities = mapper.toJpaVariants(aggregate);
        jpaVariantRepo.saveAll(vEntities);
        // Persist any processing tasks present on the aggregate that have no DB row yet
        if (aggregate.getProcessingTasks() != null && !aggregate.getProcessingTasks().isEmpty()) {
            for (DomainEntityImageProcessingTask task : aggregate.getProcessingTasks()) {
                InfrastructureJpaImageProcessingTaskEntity taskEntity = mapper.toJpaTask(task);
                jpaTaskRepo.save(taskEntity);
                if (task.toRequestedVariants() != null && !task.toRequestedVariants().isEmpty()) {
                    List<InfrastructureJpaTaskRequestedVariantEntity> req = mapper.toJpaRequestedVariants(task.getTaskId(), task.toRequestedVariants());
                    jpaTaskReqRepo.saveAll(req);
                }
            }
        }
        // Reload and map back to domain to reflect DB state
        return findById(saved.getImageId()).orElseThrow(() -> new IllegalStateException("Failed to rehydrate after save"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DomainAggregateItemImage> findById(UUID imageId) {
        Optional<InfrastructureJpaItemImageEntity> imgOpt = jpaItemRepo.findById(imageId);
        if (!imgOpt.isPresent()) return Optional.empty();
        List<InfrastructureJpaImageVariantEntity> variants = jpaVariantRepo.findByImageId(imageId);
        List<InfrastructureJpaImageProcessingTaskEntity> tasks = jpaTaskRepo.findByImageId(imageId);
        List<InfrastructureJpaTaskRequestedVariantEntity> requested = tasks.stream()
                .flatMap(t -> jpaTaskReqRepo.findByTaskId(t.getTaskId()).stream())
                .collect(Collectors.toList());
        DomainAggregateItemImage agg = mapper.toDomainAggregate(imgOpt.get(), variants, tasks, requested);
        return Optional.ofNullable(agg);
    }

    @Override
    @Transactional(readOnly = true)
    public DomainValuePage<DomainAggregateItemImage> listByItemIdWithFilters(UUID itemId,
                                                                             SharedEnumImageStatus statusFilter,
                                                                             SharedEnumImageRole roleFilter,
                                                                             int page,
                                                                             int size,
                                                                             String sort) {
        Pageable pageable = toPageable(page, size, sort);
        String status = statusFilter == null ? null : statusFilter.name();
        String role = roleFilter == null ? null : roleFilter.name();
        Page<InfrastructureJpaItemImageEntity> jpaPage = jpaItemRepo.findAllByItemIdAndFilters(itemId, status, role, pageable);
        List<DomainAggregateItemImage> content = jpaPage.getContent().stream().map(e -> {
            List<InfrastructureJpaImageVariantEntity> variants = jpaVariantRepo.findByImageId(e.getImageId());
            List<InfrastructureJpaImageProcessingTaskEntity> tasks = jpaTaskRepo.findByImageId(e.getImageId());
            List<InfrastructureJpaTaskRequestedVariantEntity> requested = tasks.stream()
                    .flatMap(t -> jpaTaskReqRepo.findByTaskId(t.getTaskId()).stream())
                    .collect(Collectors.toList());
            return mapper.toDomainAggregate(e, variants, tasks, requested);
        }).collect(Collectors.toList());
        return mapper.toDomainPage(jpaPage.getNumber(), jpaPage.getSize(), jpaPage.getTotalElements(), jpaPage.getTotalPages(), content);
    }

    private Pageable toPageable(int page, int size, String sort) {
        if (sort == null || sort.isEmpty()) {
            return PageRequest.of(page, size);
        }
        // parse format like "createdAt,desc" or multiple fields separated by ';' or ','
        Sort sortObj = Sort.unsorted();
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction dir = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")) ? Sort.Direction.DESC : Sort.Direction.ASC;
        sortObj = Sort.by(dir, property);
        return PageRequest.of(page, size, sortObj);
    }
}

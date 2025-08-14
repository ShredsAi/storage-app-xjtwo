package ai.shreds.infrastructure.repositories.impl;

import ai.shreds.domain.entities.DomainEntityImageVariant;
import ai.shreds.domain.ports.DomainOutputPortImageVariantRepository;
import ai.shreds.infrastructure.repositories.jpa.entities.InfrastructureJpaImageVariantEntity;
import ai.shreds.infrastructure.repositories.jpa.spring.InfrastructureJpaImageVariantRepository;
import ai.shreds.infrastructure.repositories.mappers.InfrastructureMapperJpaToDomain;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class InfrastructureRepositoryImplImageVariantJpa implements DomainOutputPortImageVariantRepository {

    private final InfrastructureJpaImageVariantRepository jpaVariantRepo;
    private final InfrastructureMapperJpaToDomain mapper;

    public InfrastructureRepositoryImplImageVariantJpa(InfrastructureJpaImageVariantRepository jpaVariantRepo,
                                                       InfrastructureMapperJpaToDomain mapper) {
        this.jpaVariantRepo = jpaVariantRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void saveAll(UUID imageId, Set<DomainEntityImageVariant> variants) {
        jpaVariantRepo.saveAll(variants.stream().map(v -> {
            InfrastructureJpaImageVariantEntity e = new InfrastructureJpaImageVariantEntity();
            e.setVariantId(v.getVariantId());
            e.setImageId(imageId);
            e.setType(v.getType().name());
            e.setFormat(v.getFormat().name());
            e.setWidth(v.getDimensions().getWidth());
            e.setHeight(v.getDimensions().getHeight());
            e.setFileSizeBytes(v.getFileSizeBytes());
            e.setQuality(v.getQuality());
            e.setStorageKey(v.getStorageKey());
            return e;
        }).collect(Collectors.toList()));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<DomainEntityImageVariant> findByImageId(UUID imageId) {
        return jpaVariantRepo.findByImageId(imageId).stream()
                .map(mapper::toDomainVariant)
                .collect(Collectors.toSet());
    }
}

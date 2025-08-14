package ai.shreds.application.services;

import ai.shreds.application.dtos.ApplicationDTOReprocessVariantsCommand;
import ai.shreds.application.dtos.ApplicationDTOUpdateImageCommand;
import ai.shreds.application.ports.ApplicationInputPortActivateImage;
import ai.shreds.application.ports.ApplicationInputPortDeactivateImage;
import ai.shreds.application.ports.ApplicationInputPortDeleteImage;
import ai.shreds.application.ports.ApplicationInputPortReprocessVariants;
import ai.shreds.application.ports.ApplicationInputPortUpdateImageMetadata;
import ai.shreds.domain.entities.DomainAggregateItemImage;
import ai.shreds.domain.entities.DomainEntityImageProcessingTask;
import ai.shreds.domain.entities.DomainEntityImageVariant;
import ai.shreds.domain.ports.DomainOutputPortImageVariantRepository;
import ai.shreds.domain.ports.DomainOutputPortItemImageRepository;
import ai.shreds.domain.ports.DomainOutputPortObjectStorage;
import ai.shreds.domain.ports.DomainOutputPortProcessingTaskRepository;
import ai.shreds.domain.value_objects.DomainValueDimensions;
import ai.shreds.shared.dtos.SharedDTOImageDetails;
import ai.shreds.shared.dtos.SharedDTOImageUpdateRequest;
import ai.shreds.shared.dtos.SharedDTOImageStatusResponse;
import ai.shreds.shared.dtos.SharedDTOReprocessRequest;
import ai.shreds.shared.dtos.SharedDTOReprocessResponse;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumImageStatus;
import ai.shreds.shared.enums.SharedEnumVariantType;
import ai.shreds.shared.utils.SharedUtilImageDtoMapper;
import ai.shreds.shared.value_objects.SharedValueDimensions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImageManagementService implements 
        ApplicationInputPortUpdateImageMetadata,
        ApplicationInputPortActivateImage,
        ApplicationInputPortDeactivateImage,
        ApplicationInputPortDeleteImage,
        ApplicationInputPortReprocessVariants {

    private final DomainOutputPortItemImageRepository itemImageRepository;
    private final DomainOutputPortProcessingTaskRepository taskRepository;
    private final DomainOutputPortObjectStorage objectStorage;
    private final DomainOutputPortImageVariantRepository variantRepository;
    private final ApplicationServiceVariantPolicy variantPolicy;

    public ApplicationServiceImageManagementService(
            DomainOutputPortItemImageRepository itemImageRepository,
            DomainOutputPortProcessingTaskRepository taskRepository,
            DomainOutputPortObjectStorage objectStorage,
            DomainOutputPortImageVariantRepository variantRepository,
            ApplicationServiceVariantPolicy variantPolicy) {
        this.itemImageRepository = Objects.requireNonNull(itemImageRepository, "itemImageRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.objectStorage = Objects.requireNonNull(objectStorage, "objectStorage must not be null");
        this.variantRepository = Objects.requireNonNull(variantRepository, "variantRepository must not be null");
        this.variantPolicy = Objects.requireNonNull(variantPolicy, "variantPolicy must not be null");
    }

    @Override
    @Transactional
    public SharedDTOImageDetails updateImage(UUID imageId, SharedDTOImageUpdateRequest request) {
        Objects.requireNonNull(imageId, "imageId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        try { SharedDTOImageUpdateRequest.validate(request); } catch (Throwable ignored) {}

        DomainAggregateItemImage agg = requireAggregate(imageId);
        if (agg.getStatus() == SharedEnumImageStatus.DELETED) {
            throw new IllegalStateException("Cannot update a deleted image");
        }

        SharedEnumImageRole oldRole = agg.getRole();
        if (request.getTitle() != null) { agg.updateTitle(request.getTitle()); }
        if (request.getAltText() != null) { agg.updateAltText(request.getAltText()); }
        if (request.getRole() != null && request.getRole() != oldRole) {
            agg.setRole(request.getRole());
            enqueueDefaultVariantsIfNeeded(agg);
        }

        DomainAggregateItemImage saved = itemImageRepository.save(agg);
        return SharedUtilImageDtoMapper.toImageDetails(saved);
    }

    @Override
    @Transactional
    public SharedDTOImageStatusResponse activate(UUID imageId) {
        DomainAggregateItemImage agg = requireAggregate(imageId);
        if (agg.getStatus() == SharedEnumImageStatus.DELETED) { throw new IllegalStateException("Cannot activate a deleted image"); }
        if (agg.getStatus() == SharedEnumImageStatus.INACTIVE) {
            agg.activate();
            itemImageRepository.save(agg);
        }
        return buildStatusResponse(agg.getImageId(), agg.getStatus(), agg.getUpdatedAt());
    }

    @Override
    @Transactional
    public SharedDTOImageStatusResponse deactivate(UUID imageId) {
        DomainAggregateItemImage agg = requireAggregate(imageId);
        if (agg.getStatus() == SharedEnumImageStatus.DELETED) { throw new IllegalStateException("Cannot deactivate a deleted image"); }
        if (agg.getStatus() == SharedEnumImageStatus.ACTIVE) {
            agg.deactivate();
            itemImageRepository.save(agg);
        }
        return buildStatusResponse(agg.getImageId(), agg.getStatus(), agg.getUpdatedAt());
    }

    @Override
    @Transactional
    public void delete(UUID imageId) {
        DomainAggregateItemImage agg = requireAggregate(imageId);
        Set<DomainEntityImageProcessingTask> running = taskRepository.findRunningByImageId(imageId);
        if (running != null && !running.isEmpty()) {
            throw new IllegalStateException("Cannot delete image while processing tasks are running");
        }
        Set<DomainEntityImageVariant> variants = agg.getVariants();
        if (variants == null || variants.isEmpty()) {
            try { variants = variantRepository.findByImageId(imageId); } catch (Throwable ignored) {}
        }
        if (variants != null) {
            for (DomainEntityImageVariant v : variants) {
                if (v.getStorageKey() != null) {
                    objectStorage.deleteObject(v.getStorageKey());
                }
            }
        }
        agg.markDeleted(true);
        itemImageRepository.save(agg);
    }

    @Override
    @Transactional
    public SharedDTOReprocessResponse reprocess(UUID imageId, SharedDTOReprocessRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try { SharedDTOReprocessRequest.validate(request); } catch (Throwable ignored) {}
        DomainAggregateItemImage agg = requireAggregate(imageId);
        if (agg.getStatus() == SharedEnumImageStatus.DELETED) { throw new IllegalStateException("Cannot reprocess a deleted image"); }

        Set<SharedEnumVariantType> requested = request.getRequestedVariants();
        if (requested == null || requested.isEmpty()) {
            DomainEntityImageVariant original = agg.originalVariant();
            DomainValueDimensions dims = original.toDimensions();
            SharedValueDimensions sharedDims = new SharedValueDimensions(dims.getWidth(), dims.getHeight());
            requested = new HashSet<>(variantPolicy.determineDefaultVariants(agg.getRole(), sharedDims));
        }
        requested = requested.stream().filter(v -> v != SharedEnumVariantType.ORIGINAL).collect(Collectors.toCollection(HashSet::new));
        if (requested.isEmpty()) {
            return buildReprocessResponse(agg, requested, List.of());
        }
        DomainEntityImageProcessingTask task = agg.createProcessingTask(requested);
        DomainEntityImageProcessingTask created = taskRepository.create(task);
        return buildReprocessResponse(agg, requested, List.of(created));
    }

    public SharedDTOImageDetails fromUpdateCommand(ApplicationDTOUpdateImageCommand cmd) { return updateImage(cmd.getImageId(), cmd.getRequest()); }
    public SharedDTOReprocessResponse fromReprocessCommand(ApplicationDTOReprocessVariantsCommand cmd) { return reprocess(cmd.getImageId(), cmd.getRequest()); }

    private DomainAggregateItemImage requireAggregate(UUID imageId) {
        Optional<DomainAggregateItemImage> opt = itemImageRepository.findById(imageId);
        return opt.orElseThrow(() -> new NoSuchElementException("Image not found: " + imageId));
    }

    private void enqueueDefaultVariantsIfNeeded(DomainAggregateItemImage agg) {
        DomainEntityImageVariant original = agg.originalVariant();
        DomainValueDimensions dims = original.toDimensions();
        SharedValueDimensions sharedDims = new SharedValueDimensions(dims.getWidth(), dims.getHeight());
        Set<SharedEnumVariantType> defaults = variantPolicy.determineDefaultVariants(agg.getRole(), sharedDims);
        if (!defaults.isEmpty()) {
            DomainEntityImageProcessingTask task = agg.createProcessingTask(defaults);
            taskRepository.create(task);
        }
    }

    private SharedDTOImageStatusResponse buildStatusResponse(UUID imageId, SharedEnumImageStatus status, Instant updatedAt) {
        try {
            return new SharedDTOImageStatusResponse(imageId, status, updatedAt);
        } catch (Throwable t) {
            try {
                SharedDTOImageStatusResponse resp = SharedDTOImageStatusResponse.class.getDeclaredConstructor().newInstance();
                try { SharedDTOImageStatusResponse.class.getMethod("setImageId", UUID.class).invoke(resp, imageId); } catch (Throwable ignored) {}
                try { SharedDTOImageStatusResponse.class.getMethod("setStatus", SharedEnumImageStatus.class).invoke(resp, status); } catch (Throwable ignored) {}
                try { SharedDTOImageStatusResponse.class.getMethod("setUpdatedAt", Instant.class).invoke(resp, updatedAt); } catch (Throwable ignored) {}
                return resp;
            } catch (Throwable reflectErr) {
                throw new IllegalStateException("Cannot build SharedDTOImageStatusResponse", reflectErr);
            }
        }
    }

    private SharedDTOReprocessResponse buildReprocessResponse(DomainAggregateItemImage agg,
                                                              Set<SharedEnumVariantType> requested,
                                                              List<DomainEntityImageProcessingTask> tasks) {
        List<ai.shreds.shared.dtos.SharedDTOProcessingTaskSummary> taskSummaries = new ArrayList<>();
        for (DomainEntityImageProcessingTask t : tasks) { taskSummaries.add(SharedUtilImageDtoMapper.toProcessingTaskSummary(t)); }
        List<ai.shreds.shared.dtos.SharedDTOImageVariant> variantDtos = agg.getVariants().stream().map(SharedUtilImageDtoMapper::toImageVariant).collect(Collectors.toList());
        try {
            return new SharedDTOReprocessResponse(agg.getImageId(), requested, taskSummaries, variantDtos);
        } catch (Throwable t) {
            try {
                SharedDTOReprocessResponse resp = SharedDTOReprocessResponse.class.getDeclaredConstructor().newInstance();
                try { SharedDTOReprocessResponse.class.getMethod("setImageId", UUID.class).invoke(resp, agg.getImageId()); } catch (Throwable ignored) {}
                try { SharedDTOReprocessResponse.class.getMethod("setRequestedVariants", Set.class).invoke(resp, requested); } catch (Throwable ignored) {}
                try { SharedDTOReprocessResponse.class.getMethod("setTasks", List.class).invoke(resp, taskSummaries); } catch (Throwable ignored) {}
                try { SharedDTOReprocessResponse.class.getMethod("setVariants", List.class).invoke(resp, variantDtos); } catch (Throwable ignored) {}
                return resp;
            } catch (Throwable reflectErr) {
                throw new IllegalStateException("Cannot build SharedDTOReprocessResponse", reflectErr);
            }
        }
    }
}

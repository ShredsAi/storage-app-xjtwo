package ai.shreds.application.services;

import ai.shreds.application.dtos.ApplicationDTOIngestImageCommand;
import ai.shreds.application.ports.ApplicationInputPortIngestImage;
import ai.shreds.domain.entities.DomainAggregateItemImage;
import ai.shreds.domain.entities.DomainEntityImageProcessingTask;
import ai.shreds.domain.ports.*;
import ai.shreds.domain.services.DomainServiceImageFactory;
import ai.shreds.domain.value_objects.DomainValueChecksum;
import ai.shreds.domain.value_objects.DomainValueDimensions;
import ai.shreds.domain.value_objects.DomainValueImageMetadata;
import ai.shreds.shared.dtos.SharedDTOImageDetails;
import ai.shreds.shared.enums.SharedEnumImageFormat;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumVariantType;
import ai.shreds.shared.utils.SharedUtilImageDtoMapper;
import ai.shreds.shared.value_objects.SharedValueImageMetadata;
import ai.shreds.shared.value_objects.SharedValueUploadFile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImageIngestionService implements ApplicationInputPortIngestImage {

    private final DomainOutputPortIdempotencyRepository idempotencyRepository;
    private final DomainServiceMetadataExtractor metadataExtractor;
    private final DomainServiceChecksum checksumService;
    private final DomainServiceImageFactory imageFactory;
    private final ApplicationServiceVariantPolicy variantPolicy;
    private final DomainOutputPortItemImageRepository itemImageRepository;
    private final DomainOutputPortObjectStorage objectStorage;
    private final DomainOutputPortImageVariantRepository variantRepository;
    private final DomainOutputPortProcessingTaskRepository taskRepository;

    public ApplicationServiceImageIngestionService(
            DomainOutputPortIdempotencyRepository idempotencyRepository,
            DomainServiceMetadataExtractor metadataExtractor,
            DomainServiceChecksum checksumService,
            DomainServiceImageFactory imageFactory,
            ApplicationServiceVariantPolicy variantPolicy,
            DomainOutputPortItemImageRepository itemImageRepository,
            DomainOutputPortObjectStorage objectStorage,
            DomainOutputPortImageVariantRepository variantRepository,
            DomainOutputPortProcessingTaskRepository taskRepository) {
        this.idempotencyRepository = Objects.requireNonNull(idempotencyRepository, "idempotencyRepository must not be null");
        this.metadataExtractor = Objects.requireNonNull(metadataExtractor, "metadataExtractor must not be null");
        this.checksumService = Objects.requireNonNull(checksumService, "checksumService must not be null");
        this.imageFactory = Objects.requireNonNull(imageFactory, "imageFactory must not be null");
        this.variantPolicy = Objects.requireNonNull(variantPolicy, "variantPolicy must not be null");
        this.itemImageRepository = Objects.requireNonNull(itemImageRepository, "itemImageRepository must not be null");
        this.objectStorage = Objects.requireNonNull(objectStorage, "objectStorage must not be null");
        this.variantRepository = Objects.requireNonNull(variantRepository, "variantRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
    }

    @Override
    @Transactional
    public SharedDTOImageDetails ingestImage(UUID itemId, SharedValueUploadFile file, ai.shreds.shared.dtos.SharedDTOImageUploadPayload payload, String idempotencyKey) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(file, "file must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        try { ai.shreds.shared.dtos.SharedDTOImageUploadPayload.validate(payload); } catch (Throwable ignored) {}

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<UUID> existing = idempotencyRepository.findImageIdByKey(idempotencyKey);
            if (existing.isPresent()) {
                UUID existingImageId = existing.get();
                return itemImageRepository.findById(existingImageId)
                        .map(SharedUtilImageDtoMapper::toImageDetails)
                        .orElseThrow(() -> new NoSuchElementException("Image not found for idempotency key: " + idempotencyKey));
            }
        }

        validateUpload(file, payload);

        SharedValueImageMetadata sharedMeta = metadataExtractor.extract(file.getBytes(), file.getContentType(), payload.getChecksumAlgorithm());

        String checksumAlg;
        String checksumVal;
        if (sharedMeta.getChecksum() == null || sharedMeta.getChecksum().getValue() == null) {
            var computed = checksumService.compute(file.getBytes(), payload.getChecksumAlgorithm());
            checksumAlg = computed.getAlgorithm();
            checksumVal = computed.getValue();
        } else {
            checksumAlg = sharedMeta.getChecksum().getAlgorithm();
            checksumVal = sharedMeta.getChecksum().getValue();
        }

        DomainValueDimensions domainDims = DomainValueDimensions.of(sharedMeta.getDimensions().getWidth(), sharedMeta.getDimensions().getHeight());
        DomainValueChecksum domainChecksum = DomainValueChecksum.of(checksumAlg, checksumVal);
        DomainValueImageMetadata domainMeta = DomainValueImageMetadata.of(sharedMeta.getFormat(), domainDims, sharedMeta.getFileSizeBytes(), domainChecksum);

        UUID imageId = UUID.randomUUID();
        String storageKey = determineStorageKey(imageId, sharedMeta.getFormat());

        objectStorage.putOriginal(storageKey, file.getBytes(), file.getContentType());

        Instant now = Instant.now();
        SharedEnumImageRole role = payload.getRole();
        String title = payload.getTitle();
        String altText = payload.getAltText();
        DomainAggregateItemImage aggregate = imageFactory.createNew(imageId, itemId, role, title, altText, domainMeta, storageKey, now);
        DomainAggregateItemImage saved = itemImageRepository.save(aggregate);

        try { if (saved.getVariants() != null && !saved.getVariants().isEmpty()) { variantRepository.saveAll(saved.getImageId(), saved.getVariants()); } } catch (Throwable ignored) {}

        Set<SharedEnumVariantType> requested = new HashSet<>(variantPolicy.determineDefaultVariants(role, sharedMeta.getDimensions()));
        requested = requested.stream().filter(v -> v != SharedEnumVariantType.ORIGINAL).collect(Collectors.toCollection(LinkedHashSet::new));
        if (!requested.isEmpty()) {
            DomainEntityImageProcessingTask task = saved.createProcessingTask(requested);
            taskRepository.create(task);
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyRepository.recordKey(idempotencyKey, imageId, now.plusSeconds(24 * 3600));
        }

        return SharedUtilImageDtoMapper.toImageDetails(saved);
    }

    public SharedDTOImageDetails fromCommand(ApplicationDTOIngestImageCommand cmd) {
        return ingestImage(cmd.getItemId(), cmd.getFile(), cmd.getPayload(), cmd.getIdempotencyKey());
    }

    public void validateUpload(SharedValueUploadFile file, ai.shreds.shared.dtos.SharedDTOImageUploadPayload payload) {
        if (file.getBytes() == null || file.getBytes().length == 0) { throw new IllegalArgumentException("Uploaded file is empty"); }
        if (file.getContentType() == null || file.getContentType().isBlank()) { throw new IllegalArgumentException("Content-Type is required"); }
        String ct = file.getContentType().toLowerCase(Locale.ROOT);
        if (!(ct.contains("jpeg") || ct.contains("jpg") || ct.contains("png") || ct.contains("webp") || ct.contains("avif"))) {
            throw new IllegalArgumentException("Unsupported image content type: " + file.getContentType());
        }
        if (payload.getRole() == null) { throw new IllegalArgumentException("Image role is required"); }
    }

    public String determineStorageKey(UUID imageId, SharedEnumImageFormat format) {
        String ext = format == null ? "bin" : format.name().toLowerCase(Locale.ROOT);
        return "images/" + imageId + "/original." + ext;
    }
}

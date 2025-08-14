package ai.shreds.application.services;

import ai.shreds.application.dtos.ApplicationDTOListImagesQuery;
import ai.shreds.application.ports.ApplicationInputPortGetImage;
import ai.shreds.application.ports.ApplicationInputPortListImages;
import ai.shreds.domain.entities.DomainAggregateItemImage;
import ai.shreds.domain.ports.DomainOutputPortItemImageRepository;
import ai.shreds.domain.value_objects.DomainValuePage;
import ai.shreds.shared.dtos.SharedDTOImageDetails;
import ai.shreds.shared.dtos.SharedDTOImageSummary;
import ai.shreds.shared.dtos.SharedDTOPaginatedImages;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumImageStatus;
import ai.shreds.shared.utils.SharedUtilImageDtoMapper;
import ai.shreds.shared.value_objects.SharedValueListQueryParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImageQueryService implements ApplicationInputPortListImages, ApplicationInputPortGetImage {

    private final DomainOutputPortItemImageRepository itemImageRepository;

    public ApplicationServiceImageQueryService(DomainOutputPortItemImageRepository itemImageRepository) {
        this.itemImageRepository = Objects.requireNonNull(itemImageRepository, "itemImageRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public SharedDTOPaginatedImages listImages(UUID itemId, SharedValueListQueryParams params) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(params, "params must not be null");

        SharedEnumImageStatus status = null;
        if (params.getStatus() != null && !params.getStatus().isBlank()) {
            status = SharedEnumImageStatus.valueOf(params.getStatus().trim().toUpperCase(Locale.ROOT));
        }
        SharedEnumImageRole role = params.getRole();
        int page = params.getPage();
        int size = params.getSize();
        String sort = params.getSort();

        DomainValuePage<DomainAggregateItemImage> pageResult = itemImageRepository
                .listByItemIdWithFilters(itemId, status, role, page, size, sort);

        List<SharedDTOImageSummary> summaries = pageResult.getContent().stream()
                .map(SharedUtilImageDtoMapper::toImageSummary)
                .collect(Collectors.toList());

        try {
            Constructor<SharedDTOPaginatedImages> ctor = SharedDTOPaginatedImages.class.getDeclaredConstructor(int.class, int.class, long.class, int.class, List.class);
            return ctor.newInstance(pageResult.getPage(), pageResult.getSize(), pageResult.getTotalElements(), pageResult.getTotalPages(), summaries);
        } catch (Throwable ignore) {
            try {
                SharedDTOPaginatedImages resp = SharedDTOPaginatedImages.class.getDeclaredConstructor().newInstance();
                try { SharedDTOPaginatedImages.class.getMethod("setPage", int.class).invoke(resp, pageResult.getPage()); } catch (Throwable ignored) {}
                try { SharedDTOPaginatedImages.class.getMethod("setSize", int.class).invoke(resp, pageResult.getSize()); } catch (Throwable ignored) {}
                try { SharedDTOPaginatedImages.class.getMethod("setTotalElements", long.class).invoke(resp, pageResult.getTotalElements()); } catch (Throwable ignored) {}
                try { SharedDTOPaginatedImages.class.getMethod("setTotalPages", int.class).invoke(resp, pageResult.getTotalPages()); } catch (Throwable ignored) {}
                try { SharedDTOPaginatedImages.class.getMethod("setContent", List.class).invoke(resp, summaries); } catch (Throwable ignored) {}
                return resp;
            } catch (Throwable constructionError) {
                throw new IllegalStateException("Cannot build SharedDTOPaginatedImages", constructionError);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SharedDTOImageDetails getImage(UUID imageId) {
        Objects.requireNonNull(imageId, "imageId must not be null");
        DomainAggregateItemImage agg = itemImageRepository.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("Image not found: " + imageId));
        return SharedUtilImageDtoMapper.toImageDetails(agg);
    }

    public SharedDTOPaginatedImages fromQuery(ApplicationDTOListImagesQuery query) {
        return listImages(query.getItemId(), query.getParams());
    }
}

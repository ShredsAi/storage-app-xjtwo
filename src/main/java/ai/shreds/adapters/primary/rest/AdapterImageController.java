package ai.shreds.adapters.primary.rest;

import ai.shreds.application.dtos.ApplicationDTOIngestImageCommand;
import ai.shreds.application.dtos.ApplicationDTOListImagesQuery;
import ai.shreds.application.dtos.ApplicationDTOReprocessVariantsCommand;
import ai.shreds.application.dtos.ApplicationDTOUpdateImageCommand;
import ai.shreds.application.ports.ApplicationInputPortActivateImage;
import ai.shreds.application.ports.ApplicationInputPortDeactivateImage;
import ai.shreds.application.ports.ApplicationInputPortDeleteImage;
import ai.shreds.application.ports.ApplicationInputPortGetImage;
import ai.shreds.application.ports.ApplicationInputPortIngestImage;
import ai.shreds.application.ports.ApplicationInputPortListImages;
import ai.shreds.application.ports.ApplicationInputPortReprocessVariants;
import ai.shreds.application.ports.ApplicationInputPortUpdateImageMetadata;
import ai.shreds.shared.dtos.SharedDTOImageDetails;
import ai.shreds.shared.dtos.SharedDTOImageStatusResponse;
import ai.shreds.shared.dtos.SharedDTOPaginatedImages;
import ai.shreds.shared.dtos.SharedDTOReprocessResponse;
import ai.shreds.shared.dtos.SharedDTOImageUploadPayload;
import ai.shreds.shared.dtos.SharedDTOImageUpdateRequest;
import ai.shreds.shared.dtos.SharedDTOReprocessRequest;
import ai.shreds.shared.value_objects.SharedValueListQueryParams;
import ai.shreds.shared.value_objects.SharedValueUploadFile;
import ai.shreds.adapters.primary.mappers.AdapterImageApiMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
public class AdapterImageController {

    @Autowired
    private ApplicationInputPortIngestImage ingestImagePort;

    @Autowired
    private ApplicationInputPortListImages listImagesPort;

    @Autowired
    private ApplicationInputPortGetImage getImagePort;

    @Autowired
    private ApplicationInputPortUpdateImageMetadata updateImagePort;

    @Autowired
    private ApplicationInputPortActivateImage activateImagePort;

    @Autowired
    private ApplicationInputPortDeactivateImage deactivateImagePort;

    @Autowired
    private ApplicationInputPortDeleteImage deleteImagePort;

    @Autowired
    private ApplicationInputPortReprocessVariants reprocessVariantsPort;

    @Autowired
    private AdapterImageApiMapper apiMapper;

    @Value("${pagination.default-size:${paginationDefaultSize:20}}")
    private int paginationDefaultSize;

    @Value("${pagination.max-size:${paginationMaxSize:100}}")
    private int paginationMaxSize;

    @PostMapping("/items/{itemId}/images")
    public ResponseEntity<SharedDTOImageDetails> uploadImage(
            @PathVariable UUID itemId,
            @RequestPart("file") MultipartFile multipartFile,
            @RequestPart("payload") SharedDTOImageUploadPayload payload,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) throws IOException {
        payload.validate();
        SharedValueUploadFile uploadFile = apiMapper.toSharedValueUploadFile(multipartFile);
        ApplicationDTOIngestImageCommand cmd = apiMapper.toIngestCommand(itemId, uploadFile, payload, idempotencyKey);
        SharedDTOImageDetails result = ingestImagePort.ingestImage(
                cmd.getItemId(), cmd.getFile(), cmd.getPayload(), cmd.getIdempotencyKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/items/{itemId}/images")
    public ResponseEntity<SharedDTOPaginatedImages> listImages(
            @PathVariable UUID itemId,
            @ModelAttribute SharedValueListQueryParams query
    ) {
        query.normalize(paginationDefaultSize, paginationMaxSize);
        ApplicationDTOListImagesQuery appQuery = apiMapper.toListQuery(itemId, query);
        SharedDTOPaginatedImages page = listImagesPort.listImages(appQuery.getItemId(), appQuery.getParams());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<SharedDTOImageDetails> getImage(@PathVariable UUID imageId) {
        SharedDTOImageDetails details = getImagePort.getImage(imageId);
        return ResponseEntity.ok(details);
    }

    @PatchMapping("/images/{imageId}")
    public ResponseEntity<SharedDTOImageDetails> updateImage(
            @PathVariable UUID imageId,
            @RequestBody SharedDTOImageUpdateRequest request
    ) {
        request.validate();
        ApplicationDTOUpdateImageCommand cmd = apiMapper.toUpdateCommand(imageId, request);
        SharedDTOImageDetails updated = updateImagePort.updateImage(
                cmd.getImageId(), cmd.getRequest()
        );
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/images/{imageId}/activate")
    public ResponseEntity<SharedDTOImageStatusResponse> activateImage(@PathVariable UUID imageId) {
        SharedDTOImageStatusResponse resp = activateImagePort.activate(imageId);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/images/{imageId}/deactivate")
    public ResponseEntity<SharedDTOImageStatusResponse> deactivateImage(@PathVariable UUID imageId) {
        SharedDTOImageStatusResponse resp = deactivateImagePort.deactivate(imageId);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID imageId) {
        deleteImagePort.delete(imageId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/images/{imageId}/tasks/reprocess")
    public ResponseEntity<SharedDTOReprocessResponse> reprocessVariants(
            @PathVariable UUID imageId,
            @RequestBody(required = false) SharedDTOReprocessRequest request
    ) {
        if (request != null) {
            request.validate();
        } else {
            // Allow application layer to choose default variants via policy
            request = new SharedDTOReprocessRequest();
        }
        ApplicationDTOReprocessVariantsCommand cmd = apiMapper.toReprocessCommand(imageId, request);
        SharedDTOReprocessResponse resp = reprocessVariantsPort.reprocess(
                cmd.getImageId(), cmd.getRequest()
        );
        return ResponseEntity.accepted().body(resp);
    }
}

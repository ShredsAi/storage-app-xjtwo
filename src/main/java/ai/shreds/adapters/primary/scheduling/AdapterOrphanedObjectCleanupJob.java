package ai.shreds.adapters.primary.scheduling;

import ai.shreds.domain.ports.DomainOutputPortItemImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnBean(S3Client.class)
@ConditionalOnProperty(name = "cleanup.cron", matchIfMissing = false)
public class AdapterOrphanedObjectCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AdapterOrphanedObjectCleanupJob.class);

    // Matches keys like: images/{uuid}/original.(jpg|jpeg|png|webp|avif)
    private static final Pattern ORIGINAL_KEY_PATTERN = Pattern.compile("^images/([0-9a-fA-F-]{36})/original\\.(?:jpe?g|png|webp|avif)$");

    private final S3Client s3Client;
    private final String bucketName;
    private final DomainOutputPortItemImageRepository imageRepository;
    private final String listPrefix;

    public AdapterOrphanedObjectCleanupJob(
            S3Client s3Client,
            @Value("${s3.bucketName:${s3BucketName:}}") String bucketName,
            DomainOutputPortItemImageRepository imageRepository,
            @Value("${cleanup.prefix:images/}") String listPrefix
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.imageRepository = imageRepository;
        this.listPrefix = listPrefix == null || listPrefix.isBlank() ? "images/" : listPrefix;
    }

    @Scheduled(cron = "${cleanup.cron:0 0 3 * * *}")
    public void executeCleanup() {
        if (bucketName == null || bucketName.isBlank()) {
            log.warn("Cleanup skipped: bucket name not configured");
            return;
        }
        long scanned = 0;
        long orphans = 0;
        long deleted = 0;
        long failures = 0;
        Instant started = Instant.now();
        try {
            String continuationToken = null;
            do {
                ListObjectsV2Request.Builder req = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(listPrefix)
                        .maxKeys(1000);
                if (continuationToken != null) {
                    req.continuationToken(continuationToken);
                }
                ListObjectsV2Response resp = s3Client.listObjectsV2(req.build());
                for (S3Object obj : resp.contents()) {
                    scanned++;
                    String key = obj.key();
                    Optional<UUID> imageIdOpt = extractImageIdFromOriginalKey(key);
                    if (imageIdOpt.isEmpty()) {
                        continue; // not an ORIGINAL key we manage
                    }
                    UUID imageId = imageIdOpt.get();
                    boolean exists = imageRepository.findById(imageId).isPresent();
                    if (!exists) {
                        orphans++;
                        try {
                            s3Client.deleteObject(DeleteObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(key)
                                    .build());
                            deleted++;
                            log.info("Deleted orphaned object key={} bucket={}", key, bucketName);
                        } catch (SdkException e) {
                            failures++;
                            log.warn("Failed to delete orphaned object key={} bucket={} cause={}", key, bucketName, e.toString());
                        }
                    }
                }
                continuationToken = resp.isTruncated() ? resp.nextContinuationToken() : null;
            } while (continuationToken != null && !continuationToken.isBlank());
        } catch (SdkException e) {
            log.error("Cleanup failed at storage list stage: {}", e.toString());
        } catch (Exception e) {
            log.error("Cleanup unexpected failure: {}", e.toString());
        } finally {
            log.info("OrphanedObjectCleanup run completed: started={}, scanned={}, orphansFound={}, deleted={}, failures={}", started, scanned, orphans, deleted, failures);
        }
    }

    private Optional<UUID> extractImageIdFromOriginalKey(String key) {
        if (key == null) return Optional.empty();
        Matcher m = ORIGINAL_KEY_PATTERN.matcher(key);
        if (m.find()) {
            try {
                return Optional.of(UUID.fromString(m.group(1)));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}

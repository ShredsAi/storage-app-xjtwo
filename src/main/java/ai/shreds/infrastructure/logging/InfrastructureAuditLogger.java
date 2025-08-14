package ai.shreds.infrastructure.logging;

import ai.shreds.shared.enums.SharedEnumVariantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class InfrastructureAuditLogger {
    private static final Logger log = LoggerFactory.getLogger(InfrastructureAuditLogger.class);

    public void logIngestion(UUID imageId, UUID itemId, String format, long size) {
        if (imageId == null || itemId == null) return;
        log.info("audit=image_ingestion imageId={} itemId={} format={} sizeBytes={}", imageId, itemId, format, size);
    }

    public void logDeletion(UUID imageId) {
        if (imageId == null) return;
        log.info("audit=image_deletion imageId={}", imageId);
    }

    public void logReprocessRequested(UUID imageId, Set<SharedEnumVariantType> variants) {
        if (imageId == null) return;
        log.info("audit=reprocess_requested imageId={} variants={}", imageId, variants);
    }
}

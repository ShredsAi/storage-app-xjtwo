package ai.shreds.infrastructure.external_services.images;

import ai.shreds.domain.ports.DomainServiceMetadataExtractor;
import ai.shreds.domain.ports.DomainServiceChecksum;
import ai.shreds.shared.enums.SharedEnumChecksumAlgorithm;
import ai.shreds.shared.enums.SharedEnumImageFormat;
import ai.shreds.shared.value_objects.SharedValueDimensions;
import ai.shreds.shared.value_objects.SharedValueImageMetadata;
import ai.shreds.shared.value_objects.SharedValueChecksum;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;

@Service
public class InfrastructureServiceMetadataExtractorImageIO implements DomainServiceMetadataExtractor {

    private final DomainServiceChecksum checksumService;

    public InfrastructureServiceMetadataExtractorImageIO(DomainServiceChecksum checksumService) {
        this.checksumService = checksumService;
    }

    @Override
    public SharedValueImageMetadata extract(byte[] bytes, String contentType, SharedEnumChecksumAlgorithm checksumAlgorithm) {
        try (InputStream is = new ByteArrayInputStream(bytes);
             ImageInputStream iis = ImageIO.createImageInputStream(is)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new RuntimeException("Unsupported image format or no ImageIO reader found for content type: " + contentType);
            }
            ImageReader reader = readers.next();
            reader.setInput(iis);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            String formatName = reader.getFormatName();

            SharedEnumImageFormat format = detectFormat(formatName, contentType);
            SharedValueDimensions dimensions = new SharedValueDimensions(width, height);
            long fileSize = bytes.length;

            SharedValueChecksum checksum = checksumService.compute(bytes, checksumAlgorithm);

            return new SharedValueImageMetadata(format, dimensions, fileSize, checksum);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract image metadata", e);
        }
    }

    private SharedEnumImageFormat detectFormat(String formatName, String contentType) {
        if (formatName != null) {
            String up = formatName.trim().toUpperCase();
            if ("JPG".equals(up) || "JPEG".equals(up)) return SharedEnumImageFormat.JPEG;
            if ("PNG".equals(up)) return SharedEnumImageFormat.PNG;
            if ("WEBP".equals(up)) return SharedEnumImageFormat.WEBP;
            if ("AVIF".equals(up)) return SharedEnumImageFormat.AVIF;
        }
        if (contentType != null) {
            String ct = contentType.toLowerCase();
            if (ct.contains("jpeg") || ct.contains("jpg")) return SharedEnumImageFormat.JPEG;
            if (ct.contains("png")) return SharedEnumImageFormat.PNG;
            if (ct.contains("webp")) return SharedEnumImageFormat.WEBP;
            if (ct.contains("avif")) return SharedEnumImageFormat.AVIF;
        }
        throw new IllegalArgumentException("Unable to determine image format from formatName='" + formatName + "' and contentType='" + contentType + "'");
    }
}

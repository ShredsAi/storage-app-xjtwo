package ai.shreds.domain.ports;

import ai.shreds.shared.enums.SharedEnumChecksumAlgorithm;
import ai.shreds.shared.value_objects.SharedValueImageMetadata;

public interface DomainServiceMetadataExtractor {
    SharedValueImageMetadata extract(byte[] bytes, String contentType, SharedEnumChecksumAlgorithm checksumAlgorithm);
}

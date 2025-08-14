package ai.shreds.domain.ports;

import ai.shreds.shared.enums.SharedEnumChecksumAlgorithm;
import ai.shreds.shared.value_objects.SharedValueChecksum;

public interface DomainServiceChecksum {
    SharedValueChecksum compute(byte[] bytes, SharedEnumChecksumAlgorithm algorithm);
}

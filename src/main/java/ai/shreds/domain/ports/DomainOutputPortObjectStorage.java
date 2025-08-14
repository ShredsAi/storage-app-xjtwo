package ai.shreds.domain.ports;

import java.util.Optional;

/**
 * Output port for object storage operations for image binaries.
 */
public interface DomainOutputPortObjectStorage {
    void putOriginal(String key, byte[] bytes, String contentType);

    Optional<Long> headContentLength(String key);

    void deleteObject(String key);
}

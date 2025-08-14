package ai.shreds.application.ports;

import java.util.UUID;

public interface ApplicationInputPortDeleteImage {
    void delete(UUID imageId);
}

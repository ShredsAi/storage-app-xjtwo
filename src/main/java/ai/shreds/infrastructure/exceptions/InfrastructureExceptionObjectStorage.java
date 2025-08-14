package ai.shreds.infrastructure.exceptions;

/**
 * Exception type for wrapping object storage related errors (S3/MinIO).
 */
public class InfrastructureExceptionObjectStorage extends RuntimeException {

    public InfrastructureExceptionObjectStorage() {
        super();
    }

    public InfrastructureExceptionObjectStorage(String message) {
        super(message);
    }

    public InfrastructureExceptionObjectStorage(String message, Throwable cause) {
        super(message, cause);
    }

    public InfrastructureExceptionObjectStorage(Throwable cause) {
        super(cause);
    }
}

package ai.shreds.domain.exceptions;

/**
 * Thrown when a requested domain object cannot be found.
 */
public class DomainExceptionNotFound extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DomainExceptionNotFound(String message) {
        super(message);
    }

    public DomainExceptionNotFound(String message, Throwable cause) {
        super(message, cause);
    }
}

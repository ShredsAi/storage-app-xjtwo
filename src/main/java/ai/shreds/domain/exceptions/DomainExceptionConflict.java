package ai.shreds.domain.exceptions;

/**
 * Thrown when a domain operation conflicts with the current state (e.g., concurrent modification, running tasks).
 */
public class DomainExceptionConflict extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DomainExceptionConflict(String message) {
        super(message);
    }

    public DomainExceptionConflict(String message, Throwable cause) {
        super(message, cause);
    }
}

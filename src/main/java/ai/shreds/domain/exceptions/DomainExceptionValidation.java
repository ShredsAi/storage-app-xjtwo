package ai.shreds.domain.exceptions;

/**
 * Thrown when domain-level validation constraints are violated (invalid input/state).
 */
public class DomainExceptionValidation extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DomainExceptionValidation(String message) {
        super(message);
    }

    public DomainExceptionValidation(String message, Throwable cause) {
        super(message, cause);
    }
}

package ai.shreds.shared.exceptions;

/**
 * Shared validation exception used by shared DTOs and value objects when input validation fails.
 * This is an unchecked exception to avoid polluting method signatures across layers.
 */
public class SharedExceptionValidation extends RuntimeException {
    public SharedExceptionValidation() {
        super();
    }

    public SharedExceptionValidation(String message) {
        super(message);
    }

    public SharedExceptionValidation(String message, Throwable cause) {
        super(message, cause);
    }

    public SharedExceptionValidation(Throwable cause) {
        super(cause);
    }
}

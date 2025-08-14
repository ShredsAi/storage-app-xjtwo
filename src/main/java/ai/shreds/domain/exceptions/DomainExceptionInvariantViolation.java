package ai.shreds.domain.exceptions;

/**
 * Thrown when domain invariants are broken (logic/programming error or illegal state transition).
 */
public class DomainExceptionInvariantViolation extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DomainExceptionInvariantViolation(String message) {
        super(message);
    }

    public DomainExceptionInvariantViolation(String message, Throwable cause) {
        super(message, cause);
    }
}

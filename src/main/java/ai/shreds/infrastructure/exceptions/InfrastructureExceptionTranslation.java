package ai.shreds.infrastructure.exceptions;

import ai.shreds.domain.exceptions.DomainExceptionConflict;
import ai.shreds.domain.exceptions.DomainExceptionValidation;

/**
 * Translates infrastructure-level exceptions (data access, object storage, etc.) to domain exceptions.
 */
public class InfrastructureExceptionTranslation {

    /**
     * Translate data access exceptions (JPA, JDBC, etc.) into a domain validation exception by default.
     * Adjust mappings here as needed for more granular handling.
     */
    public DomainExceptionValidation translateDataAccess(Exception ex) {
        String msg = ex == null ? "Data access error" : ex.getMessage();
        return new DomainExceptionValidation(msg);
    }

    /**
     * Translate object storage exceptions into a domain conflict exception by default.
     * This is typically used when storage operations conflict with current state or fail in a non-retryable way.
     */
    public DomainExceptionConflict translateObjectStorage(Exception ex) {
        String msg = ex == null ? "Object storage error" : ex.getMessage();
        return new DomainExceptionConflict(msg);
    }
}

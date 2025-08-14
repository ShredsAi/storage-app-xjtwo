package ai.shreds.shared.dtos;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import ai.shreds.shared.enums.SharedEnumVariantType;
import ai.shreds.shared.exceptions.SharedExceptionValidation;

public class SharedDTOReprocessRequest {
    private Set<SharedEnumVariantType> requestedVariants;

    public Set<SharedEnumVariantType> getRequestedVariants() {
        return requestedVariants;
    }

    public void setRequestedVariants(Set<SharedEnumVariantType> requestedVariants) {
        this.requestedVariants = requestedVariants;
    }

    public static void validate(SharedDTOReprocessRequest request) {
        if (request == null) {
            throw new SharedExceptionValidation("Reprocess request must not be null");
        }
        if (request.requestedVariants == null || request.requestedVariants.isEmpty()) {
            throw new SharedExceptionValidation("requestedVariants must not be null or empty");
        }
        if (request.requestedVariants.contains(SharedEnumVariantType.ORIGINAL)) {
            throw new SharedExceptionValidation("ORIGINAL variant cannot be reprocessed");
        }
        // normalize to an unmodifiable EnumSet to prevent later accidental mutation
        request.requestedVariants = Collections.unmodifiableSet(EnumSet.copyOf(request.requestedVariants));
    }

    /** Instance-level convenience to mirror other DTOs' validate pattern. */
    public void validate() {
        validate(this);
    }
}

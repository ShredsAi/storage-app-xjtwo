package ai.shreds.application.services;

import ai.shreds.domain.value_objects.DomainValueDimensions;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumVariantType;
import ai.shreds.shared.value_objects.SharedValueDimensions;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class ApplicationServiceVariantPolicy {

    // Simple example policy: choose variants based on role and original size thresholds
    public Set<SharedEnumVariantType> determineDefaultVariants(SharedEnumImageRole role, SharedValueDimensions original) {
        if (role == null || original == null) {
            return EnumSet.of(SharedEnumVariantType.ORIGINAL);
        }
        // Convert to domain VO using factory method (constructors are private)
        DomainValueDimensions dims = DomainValueDimensions.of(original.getWidth(), original.getHeight());

        EnumSet<SharedEnumVariantType> set = EnumSet.of(SharedEnumVariantType.ORIGINAL);
        // Basic sizing thresholds; in real policy we might use DomainServiceVariantPolicy
        if (dims.getWidth() >= 150 && dims.getHeight() >= 150) set.add(SharedEnumVariantType.THUMBNAIL);
        if (dims.getWidth() >= 640 && dims.getHeight() >= 480) set.add(SharedEnumVariantType.SMALL);
        if (dims.getWidth() >= 1280 && dims.getHeight() >= 720) set.add(SharedEnumVariantType.MEDIUM);
        if (dims.getWidth() >= 1920 && dims.getHeight() >= 1080) set.add(SharedEnumVariantType.LARGE);

        // Role-based tweaks (example)
        if (role == SharedEnumImageRole.THUMBNAIL_ONLY) {
            return EnumSet.of(SharedEnumVariantType.ORIGINAL, SharedEnumVariantType.THUMBNAIL);
        }
        return set;
    }
}

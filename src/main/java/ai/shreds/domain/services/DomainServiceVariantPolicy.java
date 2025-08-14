package ai.shreds.domain.services;

import ai.shreds.domain.exceptions.DomainExceptionValidation;
import ai.shreds.domain.value_objects.DomainValueDimensions;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumVariantType;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Domain policy to validate scaling rules and determine default requested variants.
 */
public class DomainServiceVariantPolicy {

    // Target max dimensions per variant type (no upscaling allowed)
    private static final int THUMB_MAX_W = 150;
    private static final int THUMB_MAX_H = 150;
    private static final int SMALL_MAX_W = 400;
    private static final int SMALL_MAX_H = 400;
    private static final int MED_MAX_W = 800;
    private static final int MED_MAX_H = 800;
    private static final int LARGE_MAX_W = 1600;
    private static final int LARGE_MAX_H = 1600;

    public void validateNoUpscaling(DomainValueDimensions original, DomainValueDimensions target) {
        Objects.requireNonNull(original, "original dimensions must not be null");
        Objects.requireNonNull(target, "target dimensions must not be null");
        if (target.getWidth() > original.getWidth() || target.getHeight() > original.getHeight()) {
            throw new DomainExceptionValidation("Target dimensions must not exceed ORIGINAL dimensions");
        }
    }

    /**
     * Determine a set of non-ORIGINAL variants to request by default based on role and original size.
     */
    public Set<SharedEnumVariantType> defaultRequestedVariants(SharedEnumImageRole role, DomainValueDimensions original) {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(original, "original dimensions must not be null");

        EnumSet<SharedEnumVariantType> result = EnumSet.noneOf(SharedEnumVariantType.class);
        switch (role) {
            case THUMBNAIL_ONLY:
                addIfFits(result, SharedEnumVariantType.THUMBNAIL, original, THUMB_MAX_W, THUMB_MAX_H);
                break;
            case GALLERY:
                addIfFits(result, SharedEnumVariantType.THUMBNAIL, original, THUMB_MAX_W, THUMB_MAX_H);
                addIfFits(result, SharedEnumVariantType.SMALL, original, SMALL_MAX_W, SMALL_MAX_H);
                addIfFits(result, SharedEnumVariantType.MEDIUM, original, MED_MAX_W, MED_MAX_H);
                break;
            case PRIMARY:
            default:
                addIfFits(result, SharedEnumVariantType.THUMBNAIL, original, THUMB_MAX_W, THUMB_MAX_H);
                addIfFits(result, SharedEnumVariantType.SMALL, original, SMALL_MAX_W, SMALL_MAX_H);
                addIfFits(result, SharedEnumVariantType.MEDIUM, original, MED_MAX_W, MED_MAX_H);
                addIfFits(result, SharedEnumVariantType.LARGE, original, LARGE_MAX_W, LARGE_MAX_H);
                break;
        }
        return result;
    }

    private void addIfFits(Set<SharedEnumVariantType> out,
                           SharedEnumVariantType type,
                           DomainValueDimensions original,
                           int maxW,
                           int maxH) {
        DomainValueDimensions target = DomainValueDimensions.of(maxW, maxH);
        if (target.getWidth() <= original.getWidth() && target.getHeight() <= original.getHeight()) {
            out.add(type);
        }
    }
}

package ai.shreds.domain.entities;

import ai.shreds.domain.exceptions.DomainExceptionConflict;
import ai.shreds.domain.exceptions.DomainExceptionInvariantViolation;
import ai.shreds.domain.exceptions.DomainExceptionValidation;
import ai.shreds.domain.value_objects.DomainValueDimensions;
import ai.shreds.domain.value_objects.DomainValueImageMetadata;
import ai.shreds.shared.enums.SharedEnumImageRole;
import ai.shreds.shared.enums.SharedEnumImageStatus;
import ai.shreds.shared.enums.SharedEnumVariantType;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregate root representing an item image with variants and processing tasks.
 */
public class DomainAggregateItemImage {
    private final UUID imageId;
    private final UUID itemId;

    private SharedEnumImageRole role;
    private String title;
    private String altText;
    private SharedEnumImageStatus status;

    private final Instant createdAt;
    private Instant updatedAt;

    private final DomainValueImageMetadata metadata;

    private final Set<DomainEntityImageVariant> variants = new HashSet<>();
    private final Set<DomainEntityImageProcessingTask> processingTasks = new HashSet<>();

    public DomainAggregateItemImage(UUID imageId,
                                    UUID itemId,
                                    SharedEnumImageRole role,
                                    String title,
                                    String altText,
                                    SharedEnumImageStatus status,
                                    Instant createdAt,
                                    Instant updatedAt,
                                    DomainValueImageMetadata metadata,
                                    Collection<DomainEntityImageVariant> variants,
                                    Collection<DomainEntityImageProcessingTask> tasks) {
        if (imageId == null) throw new DomainExceptionValidation("imageId must not be null");
        if (itemId == null) throw new DomainExceptionValidation("itemId must not be null");
        if (role == null) throw new DomainExceptionValidation("role must not be null");
        if (status == null) throw new DomainExceptionValidation("status must not be null");
        if (createdAt == null) throw new DomainExceptionValidation("createdAt must not be null");
        if (updatedAt == null) throw new DomainExceptionValidation("updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) throw new DomainExceptionValidation("updatedAt must be >= createdAt");
        if (metadata == null) throw new DomainExceptionValidation("metadata must not be null");
        this.imageId = imageId;
        this.itemId = itemId;
        this.role = role;
        this.title = trimToNull(title);
        this.altText = trimToNull(altText);
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.metadata = metadata;
        if (variants != null) this.variants.addAll(variants);
        if (tasks != null) this.processingTasks.addAll(tasks);
        validateAccessibility();
        validateVariantsInvariant();
    }

    private static String trimToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private void ensureMutable() {
        if (this.status == SharedEnumImageStatus.DELETED) {
            throw new DomainExceptionInvariantViolation("Image is DELETED and immutable");
        }
    }

    private void bumpUpdatedAt() {
        Instant now = Instant.now();
        if (now.isBefore(createdAt)) now = createdAt;
        if (now.isAfter(updatedAt)) this.updatedAt = now;
    }

    private void validateAccessibility() {
        if (this.title != null && this.title.length() > 256) {
            throw new DomainExceptionValidation("title length must be <= 256");
        }
        if (this.altText != null && this.altText.length() > 512) {
            throw new DomainExceptionValidation("altText length must be <= 512");
        }
        if (this.role == SharedEnumImageRole.PRIMARY) {
            if (this.altText == null || this.altText.isBlank()) {
                throw new DomainExceptionValidation("altText must be present and non-blank for PRIMARY images");
            }
        }
    }

    private void validateVariantsInvariant() {
        // exactly one ORIGINAL
        List<DomainEntityImageVariant> originals = this.variants.stream()
                .filter(v -> v.getType() == SharedEnumVariantType.ORIGINAL)
                .collect(Collectors.toList());
        if (originals.size() != 1) {
            throw new DomainExceptionInvariantViolation("Exactly one ORIGINAL variant is required");
        }
        DomainEntityImageVariant original = originals.get(0);
        // match format/dimensions/size
        if (original.getFormat() != metadata.getFormat()) {
            throw new DomainExceptionInvariantViolation("ORIGINAL.format must equal metadata.format");
        }
        if (!original.getDimensions().equals(metadata.getDimensions())) {
            throw new DomainExceptionInvariantViolation("ORIGINAL.dimensions must equal metadata.dimensions");
        }
        if (original.getFileSizeBytes() != metadata.getFileSizeBytes()) {
            throw new DomainExceptionInvariantViolation("ORIGINAL.fileSizeBytes must equal metadata.fileSizeBytes");
        }
        // uniqueness by type
        long distinct = this.variants.stream().map(DomainEntityImageVariant::getType).distinct().count();
        if (distinct != this.variants.size()) {
            throw new DomainExceptionInvariantViolation("Variant types must be unique per image");
        }
        // no upscaling for non-ORIGINAL
        DomainValueDimensions origDims = metadata.getDimensions();
        for (DomainEntityImageVariant v : this.variants) {
            if (v.getType() != SharedEnumVariantType.ORIGINAL) {
                if (v.getDimensions().getWidth() > origDims.getWidth() || v.getDimensions().getHeight() > origDims.getHeight()) {
                    throw new DomainExceptionInvariantViolation("Variant dimensions must not exceed ORIGINAL dimensions");
                }
            }
        }
    }

    // getters
    public UUID getImageId() { return imageId; }
    public UUID getItemId() { return itemId; }
    public SharedEnumImageRole getRole() { return role; }
    public String getTitle() { return title; }
    public String getAltText() { return altText; }
    public SharedEnumImageStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public DomainValueImageMetadata getMetadata() { return metadata; }
    public Set<DomainEntityImageVariant> getVariants() { return Collections.unmodifiableSet(variants); }
    public Set<DomainEntityImageProcessingTask> getProcessingTasks() { return Collections.unmodifiableSet(processingTasks); }

    // behaviors
    public void updateTitle(String newTitle) {
        ensureMutable();
        String t = trimToNull(newTitle);
        if (t != null && t.length() > 256) {
            throw new DomainExceptionValidation("title length must be <= 256");
        }
        this.title = t;
        bumpUpdatedAt();
    }

    public void updateAltText(String newAltText) {
        ensureMutable();
        String a = trimToNull(newAltText);
        if (a != null && a.length() > 512) {
            throw new DomainExceptionValidation("altText length must be <= 512");
        }
        this.altText = a;
        validateAccessibility();
        bumpUpdatedAt();
    }

    public void setRole(SharedEnumImageRole newRole) {
        ensureMutable();
        if (newRole == null) throw new DomainExceptionValidation("role must not be null");
        this.role = newRole;
        validateAccessibility();
        bumpUpdatedAt();
    }

    public void activate() {
        ensureMutable();
        if (this.status == SharedEnumImageStatus.ACTIVE) return;
        if (this.status == SharedEnumImageStatus.INACTIVE) {
            this.status = SharedEnumImageStatus.ACTIVE;
            bumpUpdatedAt();
        } else {
            throw new DomainExceptionInvariantViolation("Cannot activate from status " + this.status);
        }
    }

    public void deactivate() {
        ensureMutable();
        if (this.status == SharedEnumImageStatus.INACTIVE) return;
        if (this.status == SharedEnumImageStatus.ACTIVE) {
            this.status = SharedEnumImageStatus.INACTIVE;
            bumpUpdatedAt();
        } else {
            throw new DomainExceptionInvariantViolation("Cannot deactivate from status " + this.status);
        }
    }

    public void markDeleted(boolean noRunningTasks) {
        ensureMutable();
        if (!noRunningTasks) {
            throw new DomainExceptionConflict("Cannot delete image while processing tasks are RUNNING");
        }
        if (this.status == SharedEnumImageStatus.DELETED) return;
        if (this.status == SharedEnumImageStatus.ACTIVE || this.status == SharedEnumImageStatus.INACTIVE) {
            this.status = SharedEnumImageStatus.DELETED;
            bumpUpdatedAt();
        } else {
            throw new DomainExceptionInvariantViolation("Invalid transition to DELETED from status " + this.status);
        }
    }

    public void addVariant(DomainEntityImageVariant variant) {
        ensureMutable();
        Objects.requireNonNull(variant, "variant must not be null");
        if (variant.getType() == SharedEnumVariantType.ORIGINAL) {
            throw new DomainExceptionInvariantViolation("Cannot add another ORIGINAL variant");
        }
        if (hasVariantType(variant.getType())) {
            throw new DomainExceptionInvariantViolation("Variant type already exists: " + variant.getType());
        }
        DomainValueDimensions orig = originalVariant().getDimensions();
        if (variant.getDimensions().getWidth() > orig.getWidth() || variant.getDimensions().getHeight() > orig.getHeight()) {
            throw new DomainExceptionInvariantViolation("Variant dimensions must not exceed ORIGINAL dimensions");
        }
        this.variants.add(variant);
        bumpUpdatedAt();
    }

    public void replaceVariant(SharedEnumVariantType type, DomainEntityImageVariant replacement) {
        ensureMutable();
        if (type == null || replacement == null) throw new DomainExceptionValidation("type and replacement must not be null");
        if (type == SharedEnumVariantType.ORIGINAL) {
            throw new DomainExceptionInvariantViolation("Cannot replace ORIGINAL variant");
        }
        if (replacement.getType() != type) {
            throw new DomainExceptionValidation("Replacement type must equal target type");
        }
        DomainEntityImageVariant existing = getVariantByType(type);
        if (existing == null) throw new DomainExceptionValidation("Variant of type " + type + " does not exist");
        this.variants.remove(existing);
        addVariant(replacement); // validates dimensions and uniqueness
        bumpUpdatedAt();
    }

    public void removeVariant(SharedEnumVariantType type) {
        ensureMutable();
        if (type == SharedEnumVariantType.ORIGINAL) {
            throw new DomainExceptionInvariantViolation("Cannot remove ORIGINAL variant");
        }
        DomainEntityImageVariant existing = getVariantByType(type);
        if (existing == null) throw new DomainExceptionValidation("Variant of type " + type + " does not exist");
        this.variants.remove(existing);
        if (this.variants.isEmpty()) {
            throw new DomainExceptionInvariantViolation("Image must keep at least one variant (ORIGINAL)");
        }
        bumpUpdatedAt();
    }

    public DomainEntityImageProcessingTask createProcessingTask(Set<SharedEnumVariantType> requestedVariants) {
        ensureMutable();
        if (requestedVariants == null || requestedVariants.isEmpty()) {
            throw new DomainExceptionValidation("requestedVariants must be non-empty");
        }
        DomainEntityImageProcessingTask task = DomainEntityImageProcessingTask.pending(this.imageId, requestedVariants, Instant.now());
        this.processingTasks.add(task);
        bumpUpdatedAt();
        return task;
    }

    public DomainValueImageMetadata toImageMetadata() { return this.metadata; }

    public DomainEntityImageVariant originalVariant() {
        return this.variants.stream()
                .filter(v -> v.getType() == SharedEnumVariantType.ORIGINAL)
                .findFirst()
                .orElseThrow(() -> new DomainExceptionInvariantViolation("Missing ORIGINAL variant"));
    }

    private boolean hasVariantType(SharedEnumVariantType type) {
        return this.variants.stream().anyMatch(v -> v.getType() == type);
    }

    private DomainEntityImageVariant getVariantByType(SharedEnumVariantType type) {
        return this.variants.stream().filter(v -> v.getType() == type).findFirst().orElse(null);
    }
}

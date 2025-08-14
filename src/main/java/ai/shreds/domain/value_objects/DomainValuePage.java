package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionValidation;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Generic pagination value object.
 */
public final class DomainValuePage<T> {
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final List<T> content;

    private DomainValuePage(int page,
                            int size,
                            long totalElements,
                            int totalPages,
                            List<T> content) {
        if (page < 0) {
            throw new DomainExceptionValidation("page must be >= 0");
        }
        if (size <= 0) {
            throw new DomainExceptionValidation("size must be > 0");
        }
        if (totalElements < 0) {
            throw new DomainExceptionValidation("totalElements must be >= 0");
        }
        if (totalPages < 0) {
            throw new DomainExceptionValidation("totalPages must be >= 0");
        }
        if (content == null) {
            throw new DomainExceptionValidation("content must not be null");
        }
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.content = Collections.unmodifiableList(content);
    }

    public static <T> DomainValuePage<T> of(int page,
                                           int size,
                                           long totalElements,
                                           int totalPages,
                                           List<T> content) {
        return new DomainValuePage<>(page, size, totalElements, totalPages, content);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public List<T> getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainValuePage)) return false;
        DomainValuePage<?> that = (DomainValuePage<?>) o;
        return page == that.page &&
                size == that.size &&
                totalElements == that.totalElements &&
                totalPages == that.totalPages &&
                Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, size, totalElements, totalPages, content);
    }

    @Override
    public String toString() {
        return "DomainValuePage{" +
                "page=" + page +
                ", size=" + size +
                ", totalElements=" + totalElements +
                ", totalPages=" + totalPages +
                ", content=" + content +
                '}';
    }
}

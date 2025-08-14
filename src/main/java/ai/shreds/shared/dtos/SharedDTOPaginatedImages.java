package ai.shreds.shared.dtos;

import java.util.List;

public class SharedDTOPaginatedImages {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<SharedDTOImageSummary> content;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<SharedDTOImageSummary> getContent() {
        return content;
    }

    public void setContent(List<SharedDTOImageSummary> content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "SharedDTOPaginatedImages{" +
                "page=" + page +
                ", size=" + size +
                ", totalElements=" + totalElements +
                ", totalPages=" + totalPages +
                ", content=" + content +
                '}';
    }
}

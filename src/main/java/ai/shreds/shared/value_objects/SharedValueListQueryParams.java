package ai.shreds.shared.value_objects;

import ai.shreds.shared.enums.SharedEnumImageRole;

public class SharedValueListQueryParams {
    private String status; // ACTIVE|INACTIVE|ALL (adapter/application will normalize)
    private SharedEnumImageRole role; // optional
    private int page = 0;
    private int size = 20;
    private String sort = "createdAt,desc";

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public SharedEnumImageRole getRole() {
        return role;
    }

    public void setRole(SharedEnumImageRole role) {
        this.role = role;
    }

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

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public void normalize(int defaultSize, int maxSize) {
        if (page < 0) page = 0;
        if (size <= 0) size = defaultSize;
        if (size > maxSize) size = maxSize;
        if (sort == null || sort.isBlank()) {
            sort = "createdAt,desc";
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
}

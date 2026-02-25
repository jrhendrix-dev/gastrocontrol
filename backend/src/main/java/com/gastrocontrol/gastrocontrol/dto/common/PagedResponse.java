// src/main/java/com/gastrocontrol/gastrocontrol/dto/common/PagedResponse.java
package com.gastrocontrol.gastrocontrol.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable pagination wrapper. Avoids leaking Spring Page serialization details to clients.
 */
public class PagedResponse<T> {

    private List<T> content;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    private boolean first;
    private boolean last;

    public static <T> PagedResponse<T> from(Page<T> page) {
        PagedResponse<T> r = new PagedResponse<>();
        r.setContent(page.getContent());
        r.setPage(page.getNumber());
        r.setSize(page.getSize());
        r.setTotalElements(page.getTotalElements());
        r.setTotalPages(page.getTotalPages());
        r.setFirst(page.isFirst());
        r.setLast(page.isLast());
        return r;
    }

    /**
     * Use this when you post-filter mapped results but want to preserve the original paging metadata.
     * Note: totalElements/totalPages remain the original values.
     */
    public static <T> PagedResponse<T> from(Page<?> originalPage, List<T> contentOverride) {
        PagedResponse<T> r = new PagedResponse<>();
        r.setContent(contentOverride);
        r.setPage(originalPage.getNumber());
        r.setSize(originalPage.getSize());
        r.setTotalElements(originalPage.getTotalElements());
        r.setTotalPages(originalPage.getTotalPages());
        r.setFirst(originalPage.isFirst());
        r.setLast(originalPage.isLast());
        return r;
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
}
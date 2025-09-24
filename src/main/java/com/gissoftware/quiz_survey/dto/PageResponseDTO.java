package com.gissoftware.quiz_survey.dto;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
public class PageResponseDTO<T> {
    private final int pageNumber;
    private final long totalElements;
    private final int totalPages;
    private final boolean last;
    private final List<T> content;

    public PageResponseDTO(Page<T> page) {
        this.pageNumber = page.getNumber();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.last = page.isLast();
        this.content = page.getContent();
    }
}

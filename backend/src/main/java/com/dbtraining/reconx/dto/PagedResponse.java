package com.dbtraining.reconx.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * TICKET-ADV053 — Tiny wrapper that flattens Spring Data Page<T> into a
 * JSON-friendly shape. Avoids exposing Spring Data internals to clients.
 */
public record PagedResponse<T>(
        @JsonProperty("content")
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <E, T> PagedResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}

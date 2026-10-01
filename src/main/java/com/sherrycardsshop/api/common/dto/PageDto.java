package com.sherrycardsshop.api.common.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

public record PageDto<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <S, T> PageDto<T> of(Page<S> page, Function<List<S>, List<T>> mapper) {
        return new PageDto<>(mapper.apply(page.getContent()), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}

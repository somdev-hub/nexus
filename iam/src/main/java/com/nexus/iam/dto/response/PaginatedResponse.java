package com.nexus.iam.dto.response;

import java.util.List;

public record PaginatedResponse<T>(
        List<T> content,
        Integer pageNo,
        Integer pageSize,
        Long totalElements,
        Integer totalPages,
        Boolean isFirst,
        Boolean isLast,
        Boolean hasNext,
        Boolean hasPrevious
) {
}


package com.flowforge.flowforge.dto;

import java.util.List;

// Cursor-based pagination response
// nextCursor is null when there are no more results
public record CursorPageResponse<T>(
        List<T> content,
        int size,
        boolean hasNext,
        String nextCursor
) {
}

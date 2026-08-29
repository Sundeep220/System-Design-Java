package com.flowforge.flowforge.specification;

import com.flowforge.flowforge.exception.InvalidSortFieldException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SortValidator {

    // Whitelist of allowed sort fields -- reject everything else
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "status",
            "maxRetries",
            "timeoutSeconds",
            "createdAt",
            "updatedAt"
    );

    public void validate(Sort sort) {
        if (sort.isUnsorted()) {
            return;
        }
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidSortFieldException(order.getProperty(), ALLOWED_SORT_FIELDS);
            }
        }
    }

    public Set<String> getAllowedFields() {
        return ALLOWED_SORT_FIELDS;
    }
}

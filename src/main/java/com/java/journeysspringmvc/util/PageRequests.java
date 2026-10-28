package com.java.journeysspringmvc.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PageRequests {

    private final static int DEFAULT_PAGE_NUMBER = 0;
    private final static int DEFAULT_PAGE_SIZE = 25;
    private final static int MAX_PAGE_SIZE = 1000;

    private PageRequests() {
    }

    // Page numbers are 1-based for clients and 0-based for Spring Data
    public static PageRequest of(String sortBy, Integer pageNumber, Integer pageSize,
                                 Set<String> sortableFields, Sort defaultSort) {
        int queryPageNumber = pageNumber == null || pageNumber <= 0 ? DEFAULT_PAGE_NUMBER : pageNumber - 1;
        int queryPageSize = pageSize == null || pageSize <= 0 || pageSize > MAX_PAGE_SIZE ? DEFAULT_PAGE_SIZE : pageSize;

        return PageRequest.of(queryPageNumber, queryPageSize, buildSort(sortBy, sortableFields, defaultSort));
    }

    // Natours-style sort string: "-price,averageRating" = price desc, then averageRating asc
    private static Sort buildSort(String sortBy, Set<String> sortableFields, Sort defaultSort) {
        if (!StringUtils.hasText(sortBy)) {
            return defaultSort;
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (String token : sortBy.split(",")) {
            String field = token.trim();
            boolean descending = field.startsWith("-");
            if (descending) {
                field = field.substring(1);
            }
            // Ignore unknown fields so a bad query param can't blow up the query
            if (sortableFields.contains(field)) {
                orders.add(descending ? Sort.Order.desc(field) : Sort.Order.asc(field));
            }
        }

        return orders.isEmpty() ? defaultSort : Sort.by(orders);
    }
}

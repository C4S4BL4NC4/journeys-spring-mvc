/*
 * Guards the Pageable that Spring builds from ?page=&size=&sort= query parameters.
 *
 * Spring Data would otherwise let clients sort by any entity attribute, including ones that
 * aren't part of the API or that force expensive joins (e.g. sort=guides.email). Services
 * pass their allow-list here, and anything else is rejected with a 400 that names the
 * fields that can be sorted on. Page size limits are set globally by spring.data.web.pageable.*.
 */
package com.java.journeysspringmvc.util;

import com.java.journeysspringmvc.exception.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

public final class Pageables {

    private Pageables() {
    }

    public static Pageable requireSortable(Pageable pageable, Set<String> sortableFields) {
        for (Sort.Order order : pageable.getSort()) {
            if (!sortableFields.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty()
                        + "'; sortable fields are " + new TreeSet<>(sortableFields));
            }
        }
        return pageable;
    }
}

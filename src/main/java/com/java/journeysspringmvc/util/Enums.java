/*
 * Lenient enum parsing for JSON request bodies, so Natours-style values work:
 * "easy" -> Difficulty.EASY, "lead-guide" -> Role.LEAD_GUIDE. Case, dashes, underscores and
 * spaces are ignored when matching. Query and path parameters get the same leniency from
 * Spring Boot's converters (registered in WebConfig). Responses keep the canonical names.
 */
package com.java.journeysspringmvc.util;

import java.util.Arrays;
import java.util.Locale;

public final class Enums {

    private Enums() {
    }

    public static <E extends Enum<E>> E lenientValueOf(Class<E> type, String value) {
        String wanted = normalize(value);
        for (E constant : type.getEnumConstants()) {
            if (normalize(constant.name()).equals(wanted)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("'" + value + "' is not one of " + Arrays.toString(type.getEnumConstants()));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }
}

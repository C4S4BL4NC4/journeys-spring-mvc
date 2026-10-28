/*
 * The difficulty levels a tour can have, as in Natours.
 *
 * Stored in the database as the enum's name (e.g. "EASY") via @Enumerated(EnumType.STRING),
 * so reordering the constants never corrupts existing rows. Used for filtering and sorting.
 * Requests may use any casing ("easy" like Natours); responses use the constant names.
 */
package com.java.journeysspringmvc.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.java.journeysspringmvc.util.Enums;

public enum Difficulty {
    EASY,
    MEDIUM,
    DIFFICULT;

    // Accepts Natours-style "easy" as well as "EASY" in request bodies
    @JsonCreator
    public static Difficulty fromJson(String value) {
        return Enums.lenientValueOf(Difficulty.class, value);
    }
}

/*
 * The difficulty levels a tour can have, as in Natours.
 *
 * Stored in the database as the enum's name (e.g. "EASY") via @Enumerated(EnumType.STRING),
 * so reordering the constants never corrupts existing rows. Used for filtering and sorting.
 */
package com.java.journeysspringmvc.entity;

public enum Difficulty {
    EASY,
    MEDIUM,
    DIFFICULT
}

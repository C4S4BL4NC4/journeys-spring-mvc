/*
 * The roles a user can have, as in Natours.
 *
 *   USER        a regular customer (every new account starts here)
 *   GUIDE       can be assigned to lead tours
 *   LEAD_GUIDE  senior guide, also assignable to tours
 *   ADMIN       full management access
 * Stored by name in the database. Only GUIDE and LEAD_GUIDE users can be set as a tour's guides.
 */
package com.java.journeysspringmvc.entity;

public enum Role {
    USER,
    GUIDE,
    LEAD_GUIDE,
    ADMIN
}

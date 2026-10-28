/*
 * The distance units the Natours geo endpoints accept in their URLs: "mi" or "km"
 * (any casing). Converts to and from kilometres, which GeoPoint calculates in.
 */
package com.java.journeysspringmvc.geo;

public enum DistanceUnit {
    MI(1.609344),
    KM(1.0);

    private final double kilometres;

    DistanceUnit(double kilometres) {
        this.kilometres = kilometres;
    }

    public double toKm(double distance) {
        return distance * kilometres;
    }

    public double fromKm(double km) {
        return km / kilometres;
    }
}

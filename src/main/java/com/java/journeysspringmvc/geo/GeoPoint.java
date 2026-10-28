/*
 * A latitude/longitude pair, with great-circle distance between points (haversine formula).
 *
 * Used by the Natours geo endpoints (tours-within and distances). parse() reads the
 * "lat,lng" form those URLs use, e.g. "34.111745,-118.113491", and rejects malformed or
 * out-of-range coordinates with a 400.
 */
package com.java.journeysspringmvc.geo;

import com.java.journeysspringmvc.exception.BadRequestException;

public record GeoPoint(double latitude, double longitude) {

    // Mean Earth radius
    public static final double EARTH_RADIUS_KM = 6371.0088;

    public GeoPoint {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BadRequestException("Latitude must be within [-90, 90] and longitude within [-180, 180]");
        }
    }

    public static GeoPoint parse(String latLng) {
        String[] parts = latLng.split(",");
        try {
            if (parts.length == 2) {
                return new GeoPoint(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
            }
        } catch (NumberFormatException ignored) {
            // falls through to the error below
        }
        throw new BadRequestException("Please provide latitude and longitude in the format lat,lng");
    }

    public double distanceKmTo(GeoPoint other) {
        double dLat = Math.toRadians(other.latitude - latitude);
        double dLng = Math.toRadians(other.longitude - longitude);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}

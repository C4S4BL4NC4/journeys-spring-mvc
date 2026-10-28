/*
 * Unit tests for GeoPoint and DistanceUnit (no Spring context).
 *
 * Checks the "lat,lng" parsing used by the geo URLs (including malformed and out-of-range
 * input, which must become 400s), the haversine distance against a known city pair, and
 * miles/kilometre conversion.
 */
package com.java.journeysspringmvc.geo;

import com.java.journeysspringmvc.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class GeoPointTest {

    @Test
    void parsesLatLng() {
        assertThat(GeoPoint.parse("34.111745, -118.113491")).isEqualTo(new GeoPoint(34.111745, -118.113491));
    }

    @Test
    void rejectsMalformedOrOutOfRangeCoordinates() {
        for (String input : new String[]{"34.1", "a,b", "1,2,3", "91,0", "0,181"}) {
            assertThatThrownBy(() -> GeoPoint.parse(input)).as(input).isInstanceOf(BadRequestException.class);
        }
    }

    @Test
    void measuresGreatCircleDistance() {
        GeoPoint london = new GeoPoint(51.5074, -0.1278);
        GeoPoint paris = new GeoPoint(48.8566, 2.3522);

        assertThat(london.distanceKmTo(paris)).isCloseTo(343.5, within(1.0));
        assertThat(london.distanceKmTo(london)).isZero();
    }

    @Test
    void convertsUnits() {
        assertThat(DistanceUnit.MI.toKm(1)).isCloseTo(1.609344, within(1e-9));
        assertThat(DistanceUnit.MI.fromKm(1.609344)).isCloseTo(1.0, within(1e-9));
        assertThat(DistanceUnit.KM.toKm(5)).isEqualTo(5.0);
    }
}

package com.mapbox.services.android.navigation.v5.location.replay;

import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import org.junit.Test;

import java.util.List;

public class ReplayRouteLocationConverterTest {


    @Test
    public void testSliceRouteWithEmptyLineString() {
        ReplayRouteLocationConverter replayRouteLocationConverter = new ReplayRouteLocationConverter(null, 100, 1);
        List<Point> result = replayRouteLocationConverter.sliceRoute(LineString.fromJson(""));

        assert (result.isEmpty());
    }

    @Test
    public void testSliceRouteWithNullLineString() {
        ReplayRouteLocationConverter replayRouteLocationConverter = new ReplayRouteLocationConverter(null, 100, 1);
        List<Point> result = replayRouteLocationConverter.sliceRoute(null);

        assert (result.isEmpty());
    }
}
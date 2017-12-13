package com.mapbox.services.android.navigation.v5.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.core.constants.Constants;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static junit.framework.Assert.assertEquals;

public class ToleranceUtilsTest extends BaseTest {

  private static final String DIRECTIONS_FIXTURE = "single_intersection.json";

  private DirectionsResponse response;

  @Before
  public void setUp() throws Exception {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String json = loadJsonFixture(DIRECTIONS_FIXTURE);
    response = gson.fromJson(json, DirectionsResponse.class);
  }

  @Test
  public void dynamicRerouteDistanceTolerance_userFarAwayFromIntersection() throws Exception {
    RouteProgress routeProgress = RouteProgress.builder()
      .directionsRoute(response.routes().get(0))
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .stepIndex(0)
      .legIndex(0)
      .build();

    // Get a point on the route step which isn't close to an intersection.
    List<Point> stepPoints = PolylineUtils.decode(response.routes().get(0).geometry(), PRECISION_6);
    Point midPoint = TurfMeasurement.midpoint(stepPoints.get(0), stepPoints.get(1));

    double tolerance = ToleranceUtils.dynamicRerouteDistanceTolerance(midPoint, routeProgress);

    assertEquals(50.0, tolerance, DELTA);
  }


  @Test
  public void dynamicRerouteDistanceTolerance_userCloseToIntersection() throws Exception {
    RouteProgress routeProgress = RouteProgress.builder()
      .directionsRoute(response.routes().get(0))
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .stepIndex(0)
      .legIndex(0)
      .build();

    double distanceToIntersection = response.routes().get(0).distance() - 39;
    LineString lineString = LineString.fromPolyline(response.routes().get(0).geometry(), Constants.PRECISION_6);
    Point closePoint
      = TurfMeasurement.along(lineString, distanceToIntersection, TurfConstants.UNIT_METERS);

    double tolerance = ToleranceUtils.dynamicRerouteDistanceTolerance(closePoint, routeProgress);

    assertEquals(25.0, tolerance, DELTA);
  }

  @Test
  public void dynamicRerouteDistanceTolerance_userJustPastTheIntersection() throws Exception {
    RouteProgress routeProgress = RouteProgress.builder()
      .directionsRoute(response.routes().get(0))
      .legDistanceRemaining(0)
      .distanceRemaining(0)
      .stepIndex(0)
      .legIndex(0)
      .build();

    double distanceToIntersection = response.routes().get(0).distance();
    LineString lineString = LineString.fromPolyline(response.routes().get(0).geometry(), Constants.PRECISION_6);
    Point closePoint
      = TurfMeasurement.along(lineString, distanceToIntersection, TurfConstants.UNIT_METERS);

    double tolerance = ToleranceUtils.dynamicRerouteDistanceTolerance(closePoint, routeProgress);

    assertEquals(25.0, tolerance, DELTA);
  }
}
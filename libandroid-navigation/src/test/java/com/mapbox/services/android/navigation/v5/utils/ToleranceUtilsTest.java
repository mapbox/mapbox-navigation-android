package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static junit.framework.Assert.assertEquals;

public class ToleranceUtilsTest extends BaseTest {

  private DirectionsRoute route;

  @Before
  public void setUp() throws Exception {
    route = buildDirectionsRoute();
  }

  @Test
  public void dynamicRerouteDistanceTolerance_userFarAwayFromIntersection() throws Exception {
    RouteProgress routeProgress = buildDefaultRouteProgress();

    // Get a point on the route step which isn't close to an intersection.
    List<Point> stepPoints = PolylineUtils.decode(route.geometry(), PRECISION_6);
    Point midPoint = TurfMeasurement.midpoint(stepPoints.get(0), stepPoints.get(1));
    double tolerance = ToleranceUtils.dynamicRerouteDistanceTolerance(midPoint, routeProgress);

    assertEquals(25.0, tolerance, DELTA);
  }


  @Test
  public void dynamicRerouteDistanceTolerance_userCloseToIntersection() throws Exception {
    RouteProgress routeProgress = buildDefaultRouteProgress();

    double distanceToIntersection = route.distance() - 39;
    LineString lineString = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    Point closePoint
      = TurfMeasurement.along(lineString, distanceToIntersection, TurfConstants.UNIT_METERS);
    double tolerance = ToleranceUtils.dynamicRerouteDistanceTolerance(closePoint, routeProgress);

    assertEquals(50.0, tolerance, DELTA);
  }

  @Test
  public void dynamicRerouteDistanceTolerance_userJustPastTheIntersection() throws Exception {
    RouteProgress routeProgress = buildDefaultRouteProgress();

    double distanceToIntersection = route.distance();
    LineString lineString = LineString.fromPolyline(route.geometry(), Constants.PRECISION_6);
    Point closePoint
      = TurfMeasurement.along(lineString, distanceToIntersection, TurfConstants.UNIT_METERS);

    double tolerance = ToleranceUtils.dynamicRerouteDistanceTolerance(closePoint, routeProgress);

    assertEquals(50.0, tolerance, DELTA);
  }
}
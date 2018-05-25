package com.mapbox.services.android.navigation.v5;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.io.IOException;
import java.util.List;

public class BaseTest {

  protected static final double DELTA = 1E-10;
  protected static final double LARGE_DELTA = 0.1;
  protected static final String ACCESS_TOKEN = "pk.XXX";

  private TestRouteBuilder routeBuilder;
  private TestRouteProgressBuilder routeProgressBuilder;
  private MockLocationBuilder locationBuilder;

  public BaseTest() {
    routeBuilder = new TestRouteBuilder();
    routeProgressBuilder = new TestRouteProgressBuilder();
    locationBuilder = new MockLocationBuilder();
  }

  protected String loadJsonFixture(String filename) throws IOException {
    return routeBuilder.loadJsonFixture(filename);
  }

  protected DirectionsRoute buildTestDirectionsRoute() throws IOException {
    return routeBuilder.buildTestDirectionsRoute(null);
  }

  protected DirectionsRoute buildTestDirectionsRoute(@Nullable String fixtureName) throws IOException {
    return routeBuilder.buildTestDirectionsRoute(fixtureName);
  }

  protected RouteProgress buildDefaultTestRouteProgress() throws Exception {
    DirectionsRoute testRoute = routeBuilder.buildTestDirectionsRoute(null);
    return routeProgressBuilder.buildDefaultTestRouteProgress(testRoute);
  }

  protected RouteProgress buildDefaultTestRouteProgress(DirectionsRoute testRoute) throws Exception {
    return routeProgressBuilder.buildDefaultTestRouteProgress(testRoute);
  }

  protected RouteProgress buildTestRouteProgress(DirectionsRoute route,
                                                 double stepDistanceRemaining,
                                                 double legDistanceRemaining,
                                                 double distanceRemaining,
                                                 int stepIndex,
                                                 int legIndex) throws Exception {
    return routeProgressBuilder.buildTestRouteProgress(
      route, stepDistanceRemaining, legDistanceRemaining, distanceRemaining, stepIndex, legIndex
    );
  }

  protected Location buildDefaultLocationUpdate(double lng, double lat) {
    return locationBuilder.buildDefaultMockLocationUpdate(lng, lat);
  }

  @NonNull
  protected Point buildPointAwayFromLocation(Location location, double distanceAway) {
    return locationBuilder.buildPointAwayFromLocation(location, distanceAway);
  }

  @NonNull
  protected Point buildPointAwayFromPoint(Point point, double distanceAway, double bearing) {
    return locationBuilder.buildPointAwayFromPoint(point, distanceAway, bearing);
  }

  @NonNull
  protected List<Point> createCoordinatesFromCurrentStep(RouteProgress progress) {
    return locationBuilder.createCoordinatesFromCurrentStep(progress);
  }
}

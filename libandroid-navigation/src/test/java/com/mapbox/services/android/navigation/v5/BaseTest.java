package com.mapbox.services.android.navigation.v5;

import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.io.IOException;

public class BaseTest {

  protected static final double DELTA = 1E-10;
  protected static final double LARGE_DELTA = 0.1;
  protected static final String ACCESS_TOKEN = "pk.XXX";

  private TestRouteBuilder routeBuilder;
  private TestRouteProgressBuilder routeProgressBuilder;

  public BaseTest() {
    routeBuilder = new TestRouteBuilder();
    routeProgressBuilder = new TestRouteProgressBuilder();
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
}

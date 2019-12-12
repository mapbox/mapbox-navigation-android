package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.navigation.base.route.model.Route;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.io.IOException;

public class BaseTest {

  protected static final double DELTA = 1E-10;
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

  protected Route buildTestDirectionsRoute() throws IOException {
    return routeBuilder.buildTestDirectionsRoute(null);
  }

  protected RouteProgress buildRouteProgress(Route route,
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

package com.mapbox.services.android.navigation.v5.routeprogress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class RouteProgressTest extends BaseTest {

  // Fixtures
  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";
  private static final String MULTI_LEG_ROUTE = "directions_two_leg_route.json";

  private DirectionsRoute route;
  private DirectionsRoute multiLegRoute;
  private RouteProgress beginningRouteProgress;
  private RouteProgress lastRouteProgress;

  @Before
  public void setup() throws Exception {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.routes().get(0);
    RouteLeg firstLeg = route.legs().get(0);

    // Multiple leg route
    body = loadJsonFixture(MULTI_LEG_ROUTE);
    response = gson.fromJson(body, DirectionsResponse.class);
    multiLegRoute = response.routes().get(0);

    beginningRouteProgress = buildBeginningOfLegRouteProgress(route);

    int stepIndex = firstLeg.steps().size() - 1;
    LegStep step = route.legs().get(0).steps().get(stepIndex);
    int legIndex = route.legs().size() - 1;
    double legDistanceRemaining = route.legs().get(0).distance();
    double stepDistanceRemaining = step.distance();

    lastRouteProgress = buildRouteProgress(route, stepDistanceRemaining,
      legDistanceRemaining, 0, stepIndex, legIndex);
  }

  @Test
  public void sanityTest() {
    assertNotNull("should not be null", beginningRouteProgress);
  }

  @Test
  public void directionsRoute_returnsDirectionsRoute() {
    assertEquals(route, beginningRouteProgress.directionsRoute());
  }

  @Test
  public void distanceRemaining_equalsRouteDistanceAtBeginning() {
    assertEquals(route.distance(), beginningRouteProgress.distanceRemaining(), LARGE_DELTA);
  }

  @Test
  public void distanceRemaining_equalsZeroAtEndOfRoute() {
    assertEquals(0, lastRouteProgress.distanceRemaining(), DELTA);
  }

  @Test
  public void fractionTraveled_equalsZeroAtBeginning() {
    assertEquals(0, beginningRouteProgress.fractionTraveled(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void fractionTraveled_equalsCorrectValueAtIntervals() throws Exception {
    for (int stepIndex = 0; stepIndex < route.legs().get(0).steps().size(); stepIndex++) {
      double stepDistanceRemaining = getFirstStep(multiLegRoute).distance();
      double legDistanceRemaining = multiLegRoute.legs().get(0).distance();
      double distanceRemaining = multiLegRoute.distance();
      RouteProgress routeProgress = buildRouteProgress(multiLegRoute, stepDistanceRemaining,
        legDistanceRemaining, distanceRemaining, stepIndex, 0);
      float fractionRemaining = (float) (routeProgress.distanceTraveled() / route.distance());

      assertEquals(fractionRemaining, routeProgress.fractionTraveled(), BaseTest.LARGE_DELTA);
    }
  }

  @Test
  public void fractionTraveled_equalsOneAtEndOfRoute() {
    assertEquals(1.0, lastRouteProgress.fractionTraveled(), DELTA);
  }

  @Test
  public void durationRemaining_equalsRouteDurationAtBeginning() {
    assertEquals(3535.2, beginningRouteProgress.durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void durationRemaining_equalsZeroAtEndOfRoute() {
    assertEquals(0, lastRouteProgress.durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsZeroAtBeginning() {
    assertEquals(0, beginningRouteProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsRouteDistanceAtEndOfRoute() {
    assertEquals(route.distance(), lastRouteProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void currentLeg_returnsCurrentLeg() {
    assertEquals(route.legs().get(0), beginningRouteProgress.currentLeg());
  }

  @Test
  public void legIndex_returnsCurrentLegIndex() {
    assertEquals(0, beginningRouteProgress.legIndex());
  }

  /*
   * Multi-leg route test
   */

  @Test
  public void multiLeg_distanceRemaining_equalsRouteDistanceAtBeginning() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(multiLegRoute.distance(), routeProgress.distanceRemaining(), LARGE_DELTA);
  }

  @Test
  public void multiLeg_distanceRemaining_equalsZeroAtEndOfRoute() throws Exception {
    RouteProgress routeProgress = buildEndOfMultiRouteProgress();

    assertEquals(0, routeProgress.distanceRemaining(), DELTA);
  }

  @Test
  public void multiLeg_fractionTraveled_equalsZeroAtBeginning() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(0, routeProgress.fractionTraveled(), BaseTest.LARGE_DELTA);
  }

  // TODO check fut
  @Test
  public void multiLeg_getFractionTraveled_equalsCorrectValueAtIntervals() throws Exception {
    for (RouteLeg leg : multiLegRoute.legs()) {
      for (int stepIndex = 0; stepIndex < leg.steps().size(); stepIndex++) {
        double stepDistanceRemaining = getFirstStep(multiLegRoute).distance();
        double legDistanceRemaining = multiLegRoute.legs().get(0).distance();
        double distanceRemaining = multiLegRoute.distance();
        RouteProgress routeProgress = buildRouteProgress(multiLegRoute, stepDistanceRemaining,
          legDistanceRemaining, distanceRemaining, stepIndex, 0);
        float fractionRemaining = (float) (routeProgress.distanceTraveled() / multiLegRoute.distance());

        assertEquals(fractionRemaining, routeProgress.fractionTraveled(), BaseTest.LARGE_DELTA);
      }
    }
  }

  @Test
  public void multiLeg_getFractionTraveled_equalsOneAtEndOfRoute() throws Exception {
    RouteProgress routeProgress = buildEndOfMultiRouteProgress();

    assertEquals(1.0, routeProgress.fractionTraveled(), DELTA);
  }

  @Test
  public void multiLeg_getDurationRemaining_equalsRouteDurationAtBeginning() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(2858.1, routeProgress.durationRemaining(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void multiLeg_getDurationRemaining_equalsZeroAtEndOfRoute() throws Exception {
    RouteProgress routeProgress = buildEndOfMultiRouteProgress();

    assertEquals(0, routeProgress.durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void multiLeg_getDistanceTraveled_equalsZeroAtBeginning() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(0, routeProgress.distanceTraveled(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void multiLeg_getDistanceTraveled_equalsRouteDistanceAtEndOfRoute() throws Exception {
    RouteProgress routeProgress = buildEndOfMultiRouteProgress();

    assertEquals(multiLegRoute.distance(), routeProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void multiLeg_getLegIndex_returnsCurrentLegIndex() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);
    routeProgress = routeProgress.toBuilder().legIndex(1).build();

    assertEquals(1, routeProgress.legIndex());
  }

  @Test
  public void remainingWaypoints_firstLegReturnsTwoWaypoints() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(2, routeProgress.remainingWaypoints());
  }

  private RouteProgress buildBeginningOfLegRouteProgress(DirectionsRoute route) throws Exception {
    LegStep step = getFirstStep(route);
    double stepDistanceRemaining = step.distance();
    double legDistanceRemaining = route.legs().get(0).distance();
    double distanceRemaining = route.distance();
    return buildRouteProgress(route, stepDistanceRemaining, legDistanceRemaining,
      distanceRemaining, 0, 0);
  }

  private RouteProgress buildEndOfMultiRouteProgress() throws Exception {
    int legIndex = multiLegRoute.legs().size() - 1;
    int stepIndex = multiLegRoute.legs().get(legIndex).steps().size() - 1;
    double stepDistanceRemaining = multiLegRoute.legs().get(0).steps().get(stepIndex).distance();
    double legDistanceRemaining = multiLegRoute.legs().get(0).distance();
    return buildRouteProgress(multiLegRoute, stepDistanceRemaining,
      legDistanceRemaining, 0, stepIndex, legIndex);
  }
}
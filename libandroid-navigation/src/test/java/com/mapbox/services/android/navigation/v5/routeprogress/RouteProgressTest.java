package com.mapbox.services.android.navigation.v5.routeprogress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class RouteProgressTest extends BaseTest {

  private static final String MULTI_LEG_ROUTE_FIXTURE = "directions_two_leg_route.json";

  @Test
  public void sanityTest() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteProgress beginningRouteProgress = buildBeginningOfLegRouteProgress(route);

    assertNotNull(beginningRouteProgress);
  }

  @Test
  public void directionsRoute_returnsDirectionsRoute() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteProgress beginningRouteProgress = buildBeginningOfLegRouteProgress(route);

    assertEquals(route, beginningRouteProgress.directionsRoute());
  }

  @Test
  public void distanceRemaining_equalsRouteDistanceAtBeginning() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteProgress beginningRouteProgress = buildBeginningOfLegRouteProgress(route);

    assertEquals(route.distance(), beginningRouteProgress.distanceRemaining(), LARGE_DELTA);
  }

  @Test
  public void distanceRemaining_equalsZeroAtEndOfRoute() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    RouteProgress lastRouteProgress = buildLastRouteProgress(route, firstLeg);

    assertEquals(0, lastRouteProgress.distanceRemaining(), DELTA);
  }

  @Test
  public void fractionTraveled_equalsZeroAtBeginning() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteProgress beginningRouteProgress = buildBeginningOfLegRouteProgress(route);

    assertEquals(0, beginningRouteProgress.fractionTraveled(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void fractionTraveled_equalsCorrectValueAtIntervals() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    DirectionsRoute multiLegRoute = buildMultipleLegRoute();
    List<Float> fractionsRemaining = new ArrayList<>();
    List<Float> routeProgressFractionsTraveled = new ArrayList<>();

    for (int stepIndex = 0; stepIndex < route.legs().get(0).steps().size(); stepIndex++) {
      double stepDistanceRemaining = getFirstStep(multiLegRoute).distance();
      double legDistanceRemaining = multiLegRoute.legs().get(0).distance();
      double distanceRemaining = multiLegRoute.distance();
      RouteProgress routeProgress = buildTestRouteProgress(multiLegRoute, stepDistanceRemaining,
        legDistanceRemaining, distanceRemaining, stepIndex, 0);
      float fractionRemaining = (float) (routeProgress.distanceTraveled() / route.distance());

      fractionsRemaining.add(fractionRemaining);
      routeProgressFractionsTraveled.add(routeProgress.fractionTraveled());
    }

    assertTrue(fractionsRemaining.equals(routeProgressFractionsTraveled));
  }

  @Test
  public void fractionTraveled_equalsOneAtEndOfRoute() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    RouteProgress lastRouteProgress = buildLastRouteProgress(route, firstLeg);

    assertEquals(1.0, lastRouteProgress.fractionTraveled(), DELTA);
  }

  @Test
  public void durationRemaining_equalsRouteDurationAtBeginning() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteProgress beginningRouteProgress = buildBeginningOfLegRouteProgress(route);

    double durationRemaining = route.duration();
    double progressDurationRemaining = beginningRouteProgress.durationRemaining();

    assertEquals(durationRemaining, progressDurationRemaining, BaseTest.DELTA);
  }

  @Test
  public void durationRemaining_equalsZeroAtEndOfRoute() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    RouteProgress lastRouteProgress = buildLastRouteProgress(route, firstLeg);

    assertEquals(0, lastRouteProgress.durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsZeroAtBeginning() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteProgress beginningRouteProgress = buildBeginningOfLegRouteProgress(route);

    assertEquals(0, beginningRouteProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsRouteDistanceAtEndOfRoute() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteLeg firstLeg = route.legs().get(0);
    RouteProgress lastRouteProgress = buildLastRouteProgress(route, firstLeg);

    assertEquals(route.distance(), lastRouteProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void currentLeg_returnsCurrentLeg() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteProgress beginningRouteProgress = buildBeginningOfLegRouteProgress(route);

    assertEquals(route.legs().get(0), beginningRouteProgress.currentLeg());
  }

  @Test
  public void legIndex_returnsCurrentLegIndex() throws Exception {
    DirectionsRoute route = buildTestDirectionsRoute();
    RouteProgress beginningRouteProgress = buildBeginningOfLegRouteProgress(route);

    assertEquals(0, beginningRouteProgress.legIndex());
  }

  @Test
  public void multiLeg_distanceRemaining_equalsRouteDistanceAtBeginning() throws Exception {
    DirectionsRoute multiLegRoute = buildMultipleLegRoute();
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
    DirectionsRoute multiLegRoute = buildMultipleLegRoute();
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(0, routeProgress.fractionTraveled(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void multiLeg_getFractionTraveled_equalsCorrectValueAtIntervals() throws Exception {
    DirectionsRoute multiLegRoute = buildMultipleLegRoute();
    List<Float> fractionsRemaining = new ArrayList<>();
    List<Float> routeProgressFractionsTraveled = new ArrayList<>();

    for (RouteLeg leg : multiLegRoute.legs()) {
      for (int stepIndex = 0; stepIndex < leg.steps().size(); stepIndex++) {
        double stepDistanceRemaining = getFirstStep(multiLegRoute).distance();
        double legDistanceRemaining = multiLegRoute.legs().get(0).distance();
        double distanceRemaining = multiLegRoute.distance();
        RouteProgress routeProgress = buildTestRouteProgress(multiLegRoute, stepDistanceRemaining,
          legDistanceRemaining, distanceRemaining, stepIndex, 0);
        float fractionRemaining = (float) (routeProgress.distanceTraveled() / multiLegRoute.distance());

        fractionsRemaining.add(fractionRemaining);
        routeProgressFractionsTraveled.add(routeProgress.fractionTraveled());
      }
    }

    assertTrue(fractionsRemaining.equals(routeProgressFractionsTraveled));
  }

  @Test
  public void multiLeg_getFractionTraveled_equalsOneAtEndOfRoute() throws Exception {
    RouteProgress routeProgress = buildEndOfMultiRouteProgress();

    assertEquals(1.0, routeProgress.fractionTraveled(), DELTA);
  }

  @Test
  public void multiLeg_getDurationRemaining_equalsRouteDurationAtBeginning() throws Exception {
    DirectionsRoute multiLegRoute = buildMultipleLegRoute();
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
    DirectionsRoute multiLegRoute = buildMultipleLegRoute();
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(0, routeProgress.distanceTraveled(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void multiLeg_getDistanceTraveled_equalsRouteDistanceAtEndOfRoute() throws Exception {
    DirectionsRoute multiLegRoute = buildMultipleLegRoute();
    RouteProgress routeProgress = buildEndOfMultiRouteProgress();

    assertEquals(multiLegRoute.distance(), routeProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void multiLeg_getLegIndex_returnsCurrentLegIndex() throws Exception {
    DirectionsRoute multiLegRoute = buildMultipleLegRoute();
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);
    routeProgress = routeProgress.toBuilder().legIndex(1).build();

    assertEquals(1, routeProgress.legIndex());
  }

  @Test
  public void remainingWaypoints_firstLegReturnsTwoWaypoints() throws Exception {
    DirectionsRoute multiLegRoute = buildMultipleLegRoute();
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(2, routeProgress.remainingWaypoints());
  }

  private DirectionsRoute buildMultipleLegRoute() throws Exception {
    String body = loadJsonFixture(MULTI_LEG_ROUTE_FIXTURE);
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    return response.routes().get(0);
  }

  private RouteProgress buildLastRouteProgress(DirectionsRoute route, RouteLeg firstLeg) throws Exception {
    int stepIndex = firstLeg.steps().size() - 1;
    LegStep step = route.legs().get(0).steps().get(stepIndex);
    int legIndex = route.legs().size() - 1;
    double legDistanceRemaining = route.legs().get(0).distance();
    double stepDistanceRemaining = step.distance();

    return buildTestRouteProgress(route, stepDistanceRemaining,
      legDistanceRemaining, 0, stepIndex, legIndex);
  }

  private LegStep getFirstStep(DirectionsRoute route) {
    return route.legs().get(0).steps().get(0);
  }

  private RouteProgress buildBeginningOfLegRouteProgress(DirectionsRoute route) throws Exception {
    LegStep step = getFirstStep(route);
    double stepDistanceRemaining = step.distance();
    double legDistanceRemaining = route.legs().get(0).distance();
    double distanceRemaining = route.distance();
    return buildTestRouteProgress(route, stepDistanceRemaining, legDistanceRemaining,
      distanceRemaining, 0, 0);
  }

  private RouteProgress buildEndOfMultiRouteProgress() throws Exception {
    DirectionsRoute multiLegRoute = buildMultipleLegRoute();

    int legIndex = multiLegRoute.legs().size() - 1;
    int stepIndex = multiLegRoute.legs().get(legIndex).steps().size() - 1;
    double stepDistanceRemaining = multiLegRoute.legs().get(0).steps().get(stepIndex).distance();
    double legDistanceRemaining = multiLegRoute.legs().get(0).distance();
    return buildTestRouteProgress(multiLegRoute, stepDistanceRemaining,
      legDistanceRemaining, 0, stepIndex, legIndex);
  }
}
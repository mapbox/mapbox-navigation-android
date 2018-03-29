package com.mapbox.services.android.navigation.v5.routeprogress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocationManager;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowLocationManager.class})
public class RouteProgressTest extends BaseTest {

  // Fixtures
  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";
  private static final String MULTI_LEG_ROUTE = "directions_two_leg_route.json";

  private DirectionsRoute route;
  private DirectionsRoute multiLegRoute;
  private RouteProgress beginningRouteProgress;
  private RouteProgress lastRouteProgress;

  @Before
  public void setup() throws IOException {
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
    lastRouteProgress = RouteProgress.builder()
      .stepDistanceRemaining(step.distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(0)
      .directionsRoute(route)
      .currentStepPoints(buildStepPointsFromGeometry(step.geometry()))
      .stepIndex(stepIndex)
      .legIndex(route.legs().size() - 1)
      .build();
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
  public void fractionTraveled_equalsCorrectValueAtIntervals() {
    for (int stepIndex = 0; stepIndex < route.legs().get(0).steps().size(); stepIndex++) {
      LegStep step = multiLegRoute.legs().get(0).steps().get(stepIndex);
      RouteProgress routeProgress = RouteProgress.builder()
        .stepDistanceRemaining(getFirstStep(multiLegRoute).distance())
        .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
        .distanceRemaining(multiLegRoute.distance())
        .directionsRoute(multiLegRoute)
        .stepIndex(stepIndex)
        .currentStepPoints(buildStepPointsFromGeometry(step.geometry()))
        .legIndex(0)
        .build();
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
  public void multiLeg_distanceRemaining_equalsRouteDistanceAtBeginning() {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(multiLegRoute.distance(), routeProgress.distanceRemaining(), LARGE_DELTA);
  }

  @Test
  public void multiLeg_distanceRemaining_equalsZeroAtEndOfRoute() {
    RouteProgress routeProgress = buildEndOfMultiRouteProgress();

    assertEquals(0, routeProgress.distanceRemaining(), DELTA);
  }

  @Test
  public void multiLeg_fractionTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(0, routeProgress.fractionTraveled(), BaseTest.LARGE_DELTA);
  }

  // TODO check fut
  @Test
  public void multiLeg_getFractionTraveled_equalsCorrectValueAtIntervals() {
    for (RouteLeg leg : multiLegRoute.legs()) {
      for (int stepIndex = 0; stepIndex < leg.steps().size(); stepIndex++) {
        LegStep step = multiLegRoute.legs().get(0).steps().get(stepIndex);
        RouteProgress routeProgress = RouteProgress.builder()
          .stepDistanceRemaining(getFirstStep(multiLegRoute).distance())
          .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
          .distanceRemaining(multiLegRoute.distance())
          .directionsRoute(multiLegRoute)
          .stepIndex(stepIndex)
          .currentStepPoints(buildStepPointsFromGeometry(step.geometry()))
          .legIndex(0)
          .build();
        float fractionRemaining = (float) (routeProgress.distanceTraveled() / multiLegRoute.distance());

        assertEquals(fractionRemaining, routeProgress.fractionTraveled(), BaseTest.LARGE_DELTA);
      }
    }
  }

  @Test
  public void multiLeg_getFractionTraveled_equalsOneAtEndOfRoute() {
    RouteProgress routeProgress = buildEndOfMultiRouteProgress();

    assertEquals(1.0, routeProgress.fractionTraveled(), DELTA);
  }

  @Test
  public void multiLeg_getDurationRemaining_equalsRouteDurationAtBeginning() {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(2858.1, routeProgress.durationRemaining(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void multiLeg_getDurationRemaining_equalsZeroAtEndOfRoute() {
    RouteProgress routeProgress = buildEndOfMultiRouteProgress();

    assertEquals(0, routeProgress.durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void multiLeg_getDistanceTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(0, routeProgress.distanceTraveled(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void multiLeg_getDistanceTraveled_equalsRouteDistanceAtEndOfRoute() {
    RouteProgress routeProgress = buildEndOfMultiRouteProgress();

    assertEquals(multiLegRoute.distance(), routeProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void multiLeg_getLegIndex_returnsCurrentLegIndex() {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);
    routeProgress = routeProgress.toBuilder().legIndex(1).build();

    assertEquals(1, routeProgress.legIndex());
  }

  @Test
  public void remainingWaypoints_firstLegReturnsTwoWaypoints() throws Exception {
    RouteProgress routeProgress = buildBeginningOfLegRouteProgress(multiLegRoute);

    assertEquals(2, routeProgress.remainingWaypoints());
  }

  private RouteProgress buildBeginningOfLegRouteProgress(DirectionsRoute route) {
    LegStep step = getFirstStep(route);
    List<Point> currentStepPoints = buildStepPointsFromGeometry(step.geometry());
    return RouteProgress.builder()
      .stepDistanceRemaining(step.distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .currentStepPoints(currentStepPoints)
      .stepIndex(0)
      .legIndex(0)
      .build();
  }

  private RouteProgress buildEndOfMultiRouteProgress() {
    int legIndex = multiLegRoute.legs().size() - 1;
    int stepIndex = multiLegRoute.legs().get(legIndex).steps().size() - 1;
    LegStep currentStep = multiLegRoute.legs().get(legIndex).steps().get(stepIndex);
    return RouteProgress.builder()
      .stepDistanceRemaining(multiLegRoute.legs().get(0).steps().get(stepIndex).distance())
      .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
      .distanceRemaining(0)
      .directionsRoute(multiLegRoute)
      .currentStepPoints(buildStepPointsFromGeometry(currentStep.geometry()))
      .stepIndex(stepIndex)
      .legIndex(legIndex)
      .build();
  }
}
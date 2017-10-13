package com.mapbox.services.android.navigation.v5.routeprogress;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.directions.v5.DirectionsAdapterFactory;
import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocationManager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.IOException;

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

    beginningRouteProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(route.distance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();

    lastRouteProgress = RouteProgress.builder()
      .stepDistanceRemaining(route.legs().get(0).steps().get(firstLeg.steps().size() - 1).distance())
      .legDistanceRemaining(route.legs().get(0).distance())
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.steps().size() - 1)
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
    // Chop the line in small pieces
    for (int step = 0; step < route.legs().get(0).steps().size(); step++) {
      RouteProgress routeProgress = RouteProgress.builder()
        .stepDistanceRemaining(route.legs().get(0).steps().get(0).distance())
        .legDistanceRemaining(route.legs().get(0).distance())
        .distanceRemaining(route.distance())
        .directionsRoute(route)
        .stepIndex(step)
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
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(multiLegRoute.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
      .distanceRemaining(multiLegRoute.distance())
      .directionsRoute(multiLegRoute)
      .stepIndex(0)
      .legIndex(0)
      .build();
    assertEquals(multiLegRoute.distance(), routeProgress.distanceRemaining(), LARGE_DELTA);
  }

  @Test
  public void multiLeg_distanceRemaining_equalsZeroAtEndOfRoute() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(multiLegRoute.legs().get(0).steps().get(multiLegRoute
        .legs().get(multiLegRoute.legs().size() - 1).steps().size() - 1).distance())
      .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
      .distanceRemaining(0)
      .directionsRoute(multiLegRoute)
      .stepIndex(multiLegRoute.legs().get(multiLegRoute.legs().size() - 1).steps().size() - 1)
      .legIndex(multiLegRoute.legs().size() - 1)
      .build();
    assertEquals(0, routeProgress.distanceRemaining(), DELTA);
  }

  @Test
  public void multiLeg_fractionTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(multiLegRoute.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
      .distanceRemaining(multiLegRoute.distance())
      .directionsRoute(multiLegRoute)
      .stepIndex(0)
      .legIndex(0)
      .build();
    assertEquals(0, routeProgress.fractionTraveled(), BaseTest.LARGE_DELTA);
  }

  // TODO check fut
  @Test
  public void multiLeg_getFractionTraveled_equalsCorrectValueAtIntervals() {
    // Chop the line in small pieces
    for (RouteLeg leg : multiLegRoute.legs()) {
      for (int step = 0; step < leg.steps().size(); step++) {
        RouteProgress routeProgress = RouteProgress.builder()
          .stepDistanceRemaining(multiLegRoute.legs().get(0).steps().get(0).distance())
          .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
          .distanceRemaining(multiLegRoute.distance())
          .directionsRoute(multiLegRoute)
          .stepIndex(step)
          .legIndex(0)
          .build();
        float fractionRemaining = (float) (routeProgress.distanceTraveled() / multiLegRoute.distance());
        assertEquals(fractionRemaining, routeProgress.fractionTraveled(), BaseTest.LARGE_DELTA);
      }
    }
  }

  @Test
  public void multiLeg_getFractionTraveled_equalsOneAtEndOfRoute() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(multiLegRoute.legs().get(0).steps().get(multiLegRoute
        .legs().get(multiLegRoute.legs().size() - 1).steps().size() - 1).distance())
      .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
      .distanceRemaining(0)
      .directionsRoute(multiLegRoute)
      .stepIndex(multiLegRoute.legs().get(multiLegRoute.legs().size() - 1).steps().size() - 1)
      .legIndex(multiLegRoute.legs().size() - 1)
      .build();
    assertEquals(1.0, routeProgress.fractionTraveled(), DELTA);
  }

  @Test
  public void multiLeg_getDurationRemaining_equalsRouteDurationAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(multiLegRoute.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
      .distanceRemaining(multiLegRoute.distance())
      .directionsRoute(multiLegRoute)
      .stepIndex(0)
      .legIndex(0)
      .build();
    assertEquals(2858.1, routeProgress.durationRemaining(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void multiLeg_getDurationRemaining_equalsZeroAtEndOfRoute() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(multiLegRoute.legs().get(0).steps().get(multiLegRoute
        .legs().get(multiLegRoute.legs().size() - 1).steps().size() - 1).distance())
      .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
      .distanceRemaining(0)
      .directionsRoute(multiLegRoute)
      .stepIndex(multiLegRoute.legs().get(multiLegRoute.legs().size() - 1).steps().size() - 1)
      .legIndex(multiLegRoute.legs().size() - 1)
      .build();
    assertEquals(0, routeProgress.durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void multiLeg_getDistanceTraveled_equalsZeroAtBeginning() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(multiLegRoute.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
      .distanceRemaining(multiLegRoute.distance())
      .directionsRoute(multiLegRoute)
      .stepIndex(0)
      .legIndex(0)
      .build();
    assertEquals(0, routeProgress.distanceTraveled(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void multiLeg_getDistanceTraveled_equalsRouteDistanceAtEndOfRoute() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(multiLegRoute.legs().get(0).steps().get(multiLegRoute
        .legs().get(multiLegRoute.legs().size() - 1).steps().size() - 1).distance())
      .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
      .distanceRemaining(0)
      .directionsRoute(multiLegRoute)
      .stepIndex(multiLegRoute.legs().get(multiLegRoute.legs().size() - 1).steps().size() - 1)
      .legIndex(multiLegRoute.legs().size() - 1)
      .build();
    assertEquals(multiLegRoute.distance(), routeProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void multiLeg_getLegIndex_returnsCurrentLegIndex() {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(multiLegRoute.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
      .distanceRemaining(multiLegRoute.distance())
      .directionsRoute(multiLegRoute)
      .stepIndex(0)
      .legIndex(1)
      .build();
    assertEquals(1, routeProgress.legIndex());
  }

  @Test
  public void remainingWaypoints_firstLegReturnsTwoWaypoints() throws Exception {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(multiLegRoute.legs().get(0).steps().get(0).distance())
      .legDistanceRemaining(multiLegRoute.legs().get(0).distance())
      .distanceRemaining(multiLegRoute.distance())
      .directionsRoute(multiLegRoute)
      .stepIndex(0)
      .legIndex(0)
      .build();

    assertEquals(2, routeProgress.remainingWaypoints());
  }
}
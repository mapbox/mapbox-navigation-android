package com.mapbox.services.android.navigation.v5.routeprogress;

import android.location.Location;

import com.google.gson.Gson;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.commons.models.Position;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocationManager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

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
  private RouteLeg firstLeg;

  @Before
  public void setup() {
    Gson gson = new Gson();
    String body = readPath(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.getRoutes().get(0);
    firstLeg = route.getLegs().get(0);

    // Multiple leg route
    body = readPath(MULTI_LEG_ROUTE);
    response = gson.fromJson(body, DirectionsResponse.class);
    multiLegRoute = response.getRoutes().get(0);

    Position firstCoordinate = route.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
    beginningRouteProgress = RouteProgress.builder()
      .location(buildTestLocation(firstCoordinate))
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(route.getDistance())
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();

    Position lastCoordinate
      = route.getLegs().get(0).getSteps().get(route.getLegs().get(0).getSteps().size() - 1).getManeuver().asPosition();

    lastRouteProgress = RouteProgress.builder()
      .location(buildTestLocation(lastCoordinate))
      .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(firstLeg.getSteps().size() - 1).getDistance())
      .legDistanceRemaining(route.getLegs().get(0).getDistance())
      .distanceRemaining(0)
      .directionsRoute(route)
      .stepIndex(firstLeg.getSteps().size() - 1)
      .legIndex(route.getLegs().size() - 1)
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
    assertEquals(route.getDistance(), beginningRouteProgress.distanceRemaining(), LARGE_DELTA);
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
    for (int step = 0; step < route.getLegs().get(0).getSteps().size(); step++) {
      Position position = route.getLegs().get(0).getSteps().get(step).getManeuver().asPosition();

      RouteProgress routeProgress = RouteProgress.builder()
        .location(buildTestLocation(position))
        .stepDistanceRemaining(route.getLegs().get(0).getSteps().get(0).getDistance())
        .legDistanceRemaining(route.getLegs().get(0).getDistance())
        .distanceRemaining(route.getDistance())
        .directionsRoute(route)
        .stepIndex(step)
        .legIndex(0)
        .build();
      float fractionRemaining = (float) (routeProgress.distanceTraveled() / route.getDistance());
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
    Assert.assertEquals(0, beginningRouteProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void distanceTraveled_equalsRouteDistanceAtEndOfRoute() {
    Assert.assertEquals(route.getDistance(), lastRouteProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void currentLeg_returnsCurrentLeg() {
    Assert.assertEquals(route.getLegs().get(0), beginningRouteProgress.currentLeg());
  }

  @Test
  public void legIndex_returnsCurrentLegIndex() {
    Assert.assertEquals(0, beginningRouteProgress.legIndex());
  }

  /*
   * Multi-leg route test
   */

  @Test
  public void multiLeg_distanceRemaining_equalsRouteDistanceAtBeginning() {
    Position firstCoordinate = multiLegRoute.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstCoordinate))
      .stepDistanceRemaining(multiLegRoute.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(multiLegRoute.getLegs().get(0).getDistance())
      .distanceRemaining(multiLegRoute.getDistance())
      .directionsRoute(multiLegRoute)
      .stepIndex(0)
      .legIndex(0)
      .build();
    Assert.assertEquals(multiLegRoute.getDistance(), routeProgress.distanceRemaining(), LARGE_DELTA);
  }

  @Test
  public void multiLeg_distanceRemaining_equalsZeroAtEndOfRoute() {
    Position lastCoordinate
      = multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1)
      .getSteps().get(multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1)
      .getManeuver().asPosition();
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(lastCoordinate))
      .stepDistanceRemaining(multiLegRoute.getLegs().get(0).getSteps().get(multiLegRoute
        .getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1).getDistance())
      .legDistanceRemaining(multiLegRoute.getLegs().get(0).getDistance())
      .distanceRemaining(0)
      .directionsRoute(multiLegRoute)
      .stepIndex(multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1)
      .legIndex(multiLegRoute.getLegs().size() - 1)
      .build();
    Assert.assertEquals(0, routeProgress.distanceRemaining(), DELTA);
  }

  @Test
  public void multiLeg_fractionTraveled_equalsZeroAtBeginning() {
    Position firstCoordinate = multiLegRoute.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstCoordinate))
      .stepDistanceRemaining(multiLegRoute.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(multiLegRoute.getLegs().get(0).getDistance())
      .distanceRemaining(multiLegRoute.getDistance())
      .directionsRoute(multiLegRoute)
      .stepIndex(0)
      .legIndex(0)
      .build();
    Assert.assertEquals(0, routeProgress.fractionTraveled(), BaseTest.LARGE_DELTA);
  }

  // TODO check fut
  @Test
  public void multiLeg_getFractionTraveled_equalsCorrectValueAtIntervals() {
    // Chop the line in small pieces
    for (RouteLeg leg : multiLegRoute.getLegs()) {
      for (int step = 0; step < leg.getSteps().size(); step++) {
        Position position = multiLegRoute.getLegs().get(0).getSteps().get(step).getManeuver().asPosition();
        RouteProgress routeProgress = RouteProgress.builder()
          .location(buildTestLocation(position))
          .stepDistanceRemaining(multiLegRoute.getLegs().get(0).getSteps().get(0).getDistance())
          .legDistanceRemaining(multiLegRoute.getLegs().get(0).getDistance())
          .distanceRemaining(multiLegRoute.getDistance())
          .directionsRoute(multiLegRoute)
          .stepIndex(step)
          .legIndex(0)
          .build();
        float fractionRemaining = (float) (routeProgress.distanceTraveled() / multiLegRoute.getDistance());
        Assert.assertEquals(fractionRemaining, routeProgress.fractionTraveled(), BaseTest.LARGE_DELTA);
      }
    }
  }

  @Test
  public void multiLeg_getFractionTraveled_equalsOneAtEndOfRoute() {
    Position lastCoordinate
      = multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1)
      .getSteps().get(multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1)
        .getSteps().size() - 1).getManeuver().asPosition();
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(lastCoordinate))
      .stepDistanceRemaining(multiLegRoute.getLegs().get(0).getSteps().get(multiLegRoute
        .getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1).getDistance())
      .legDistanceRemaining(multiLegRoute.getLegs().get(0).getDistance())
      .distanceRemaining(0)
      .directionsRoute(multiLegRoute)
      .stepIndex(multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1)
      .legIndex(multiLegRoute.getLegs().size() - 1)
      .build();
    Assert.assertEquals(1.0, routeProgress.fractionTraveled(), DELTA);
  }

  @Test
  public void multiLeg_getDurationRemaining_equalsRouteDurationAtBeginning() {
    Position firstCoordinate = multiLegRoute.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstCoordinate))
      .stepDistanceRemaining(multiLegRoute.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(multiLegRoute.getLegs().get(0).getDistance())
      .distanceRemaining(multiLegRoute.getDistance())
      .directionsRoute(multiLegRoute)
      .stepIndex(0)
      .legIndex(0)
      .build();
    Assert.assertEquals(2858.1, routeProgress.durationRemaining(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void multiLeg_getDurationRemaining_equalsZeroAtEndOfRoute() {
    Position lastCoordinate
      = multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().get(multiLegRoute.getLegs()
      .get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1).getManeuver().asPosition();
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(lastCoordinate))
      .stepDistanceRemaining(multiLegRoute.getLegs().get(0).getSteps().get(multiLegRoute
        .getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1).getDistance())
      .legDistanceRemaining(multiLegRoute.getLegs().get(0).getDistance())
      .distanceRemaining(0)
      .directionsRoute(multiLegRoute)
      .stepIndex(multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1)
      .legIndex(multiLegRoute.getLegs().size() - 1)
      .build();
    Assert.assertEquals(0, routeProgress.durationRemaining(), BaseTest.DELTA);
  }

  @Test
  public void multiLeg_getDistanceTraveled_equalsZeroAtBeginning() {
    Position firstCoordinate = multiLegRoute.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(firstCoordinate))
      .stepDistanceRemaining(multiLegRoute.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(multiLegRoute.getLegs().get(0).getDistance())
      .distanceRemaining(multiLegRoute.getDistance())
      .directionsRoute(multiLegRoute)
      .stepIndex(0)
      .legIndex(0)
      .build();
    Assert.assertEquals(0, routeProgress.distanceTraveled(), BaseTest.LARGE_DELTA);
  }

  @Test
  public void multiLeg_getDistanceTraveled_equalsRouteDistanceAtEndOfRoute() {
    Position lastCoordinate
      = multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1)
      .getSteps().get(multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1)
      .getManeuver().asPosition();
    RouteProgress routeProgress = RouteProgress.builder()
      .location(buildTestLocation(lastCoordinate))
      .stepDistanceRemaining(multiLegRoute.getLegs().get(0).getSteps().get(multiLegRoute
        .getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1).getDistance())
      .legDistanceRemaining(multiLegRoute.getLegs().get(0).getDistance())
      .distanceRemaining(0)
      .directionsRoute(multiLegRoute)
      .stepIndex(multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1)
      .legIndex(multiLegRoute.getLegs().size() - 1)
      .build();
    Assert.assertEquals(multiLegRoute.getDistance(), routeProgress.distanceTraveled(), BaseTest.DELTA);
  }

  @Test
  public void multiLeg_getLegIndex_returnsCurrentLegIndex() {
    RouteProgress routeProgress = RouteProgress.builder()
      .location(mock(Location.class))
      .stepDistanceRemaining(multiLegRoute.getLegs().get(0).getSteps().get(0).getDistance())
      .legDistanceRemaining(multiLegRoute.getLegs().get(0).getDistance())
      .distanceRemaining(multiLegRoute.getDistance())
      .directionsRoute(multiLegRoute)
      .stepIndex(0)
      .legIndex(1)
      .build();
    Assert.assertEquals(1, routeProgress.legIndex());
  }
}
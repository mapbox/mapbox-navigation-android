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
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocationManager;

import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, shadows = {ShadowLocationManager.class})
public class RouteProgressTest extends BaseTest {
//
//  // Fixtures
//  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";
//  private static final String MULTI_LEG_ROUTE = "directions_two_leg_route.json";
//
//  private DirectionsRoute route;
//  private DirectionsRoute multiLegRoute;
//  private RouteLeg firstLeg;
//
//  @Before
//  public void setup() {
//    Gson gson = new Gson();
//    String body = readPath(DIRECTIONS_PRECISION_6);
//    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
//    route = response.getRoutes().get(0);
//    firstLeg = route.getLegs().get(0);
//
//    // Multiple leg route
//    body = readPath(MULTI_LEG_ROUTE);
//    response = gson.fromJson(body, DirectionsResponse.class);
//    multiLegRoute = response.getRoutes().get(0);
//  }
//
//  @Test
//  public void sanityTest() {
//    RouteProgress routeProgress = RouteProgress.create(route, Mockito.mock(Location.class), 0, 0);
//    assertNotNull("should not be null", routeProgress);
//  }
//
//  @Test
//  public void getRoute_returnsDirectionsRoute() {
//    RouteProgress routeProgress = RouteProgress.create(route, Mockito.mock(Location.class), 0, 0);
//    Assert.assertEquals(route, routeProgress.getRoute());
//  }
//
//  @Test
//  public void getDistanceRemaining_equalsRouteDistanceAtBeginning() {
//    Position firstCoordinate = route.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(route, buildTestLocation(firstCoordinate), 0, 0);
//    Assert.assertEquals(route.getDistance(), routeProgress.getDistanceRemaining(), LARGE_DELTA);
//  }
//
//  @Test
//  public void getDistanceRemaining_equalsZeroAtEndOfRoute() {
//    Position lastCoordinate
//      = route.getLegs().get(0).getSteps().get(route.getLegs().get(0).getSteps().size() - 1).getManeuver().asPosition();
//    RouteProgress routeProgress
//      = RouteProgress.create(route, buildTestLocation(lastCoordinate), route.getLegs().size() - 1,
//      firstLeg.getSteps().size() - 1);
//    Assert.assertEquals(0, routeProgress.getDistanceRemaining(), DELTA);
//  }
//
//  @Test
//  public void getFractionTraveled_equalsZeroAtBeginning() {
//    Position firstCoordinate = route.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(route, buildTestLocation(firstCoordinate), 0, 0);
//    Assert.assertEquals(0, routeProgress.getFractionTraveled(), BaseTest.LARGE_DELTA);
//  }
//
//  @Test
//  public void getFractionTraveled_equalsCorrectValueAtIntervals() {
//    // Chop the line in small pieces
//    for (int step = 0; step < route.getLegs().get(0).getSteps().size(); step++) {
//      Position position = route.getLegs().get(0).getSteps().get(step).getManeuver().asPosition();
//      RouteProgress routeProgress = RouteProgress.create(route, buildTestLocation(position), 0, step);
//      float fractionRemaining = (float) (routeProgress.getDistanceTraveled() / route.getDistance());
//      Assert.assertEquals(fractionRemaining, routeProgress.getFractionTraveled(), BaseTest.LARGE_DELTA);
//    }
//  }
//
//  @Test
//  public void getFractionTraveled_equalsOneAtEndOfRoute() {
//    Position lastCoordinate
//      = route.getLegs().get(0).getSteps().get(route.getLegs().get(0).getSteps().size() - 1).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(
//      route, buildTestLocation(lastCoordinate), route.getLegs().size() - 1, firstLeg.getSteps().size() - 1);
//    Assert.assertEquals(1.0, routeProgress.getFractionTraveled(), DELTA);
//  }
//
//  @Test
//  public void getDurationRemaining_equalsRouteDurationAtBeginning() {
//    Position firstCoordinate = route.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(route, buildTestLocation(firstCoordinate), 0, 0);
//    Assert.assertEquals(3535.2, routeProgress.getDurationRemaining(), BaseTest.DELTA);
//  }
//
//  @Test
//  public void getDurationRemaining_equalsZeroAtEndOfRoute() {
//    Position lastCoordinate
//      = route.getLegs().get(route.getLegs().size() - 1).getSteps().get(route.getLegs().get(route.getLegs().size() - 1)
//      .getSteps().size() - 1).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(route, buildTestLocation(lastCoordinate),
//      route.getLegs().size() - 1, firstLeg.getSteps().size() - 1);
//    Assert.assertEquals(0, routeProgress.getDurationRemaining(), BaseTest.DELTA);
//  }
//
//  @Test
//  public void getDistanceTraveled_equalsZeroAtBeginning() {
//    Position firstCoordinate = route.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(route, buildTestLocation(firstCoordinate), 0, 0);
//    Assert.assertEquals(0, routeProgress.getDistanceTraveled(), BaseTest.DELTA);
//  }
//
//  @Test
//  public void getDistanceTraveled_equalsRouteDistanceAtEndOfRoute() {
//    Position lastCoordinate
//      = route.getLegs().get(0).getSteps().get(route.getLegs().get(0).getSteps().size() - 1).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(route, buildTestLocation(lastCoordinate),
//      route.getLegs().size() - 1, firstLeg.getSteps().size() - 1);
//    Assert.assertEquals(route.getDistance(), routeProgress.getDistanceTraveled(), BaseTest.DELTA);
//  }
//
//  @Test
//  public void getCurrentLeg_returnsCurrentLeg() {
//    RouteProgress routeProgress = RouteProgress.create(route, Mockito.mock(Location.class), 0, 0);
//    Assert.assertEquals(route.getLegs().get(0), routeProgress.getCurrentLeg());
//  }
//
//  @Test
//  public void getLegIndex_returnsCurrentLegIndex() {
//    RouteProgress routeProgress = RouteProgress.create(route, Mockito.mock(Location.class), 0, 0);
//    Assert.assertEquals(0, routeProgress.getLegIndex());
//  }
//
//  /*
//   * Multi-leg route test
//   */
//
//  @Test
//  public void multiLeg_getDistanceRemaining_equalsRouteDistanceAtBeginning() {
//    Position firstCoordinate = multiLegRoute.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(multiLegRoute, buildTestLocation(firstCoordinate), 0, 0);
//    Assert.assertEquals(multiLegRoute.getDistance(), routeProgress.getDistanceRemaining(), LARGE_DELTA);
//  }
//
//  @Test
//  public void multiLeg_getDistanceRemaining_equalsZeroAtEndOfRoute() {
//    Position lastCoordinate
//      = multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1)
//      .getSteps().get(multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1)
//      .getManeuver().asPosition();
//    RouteProgress routeProgress
//      = RouteProgress.create(multiLegRoute, buildTestLocation(lastCoordinate), multiLegRoute.getLegs().size() - 1,
//      multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1);
//    Assert.assertEquals(0, routeProgress.getDistanceRemaining(), DELTA);
//  }
//
//  @Test
//  public void multiLeg_getFractionTraveled_equalsZeroAtBeginning() {
//    Position firstCoordinate = multiLegRoute.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(multiLegRoute, buildTestLocation(firstCoordinate), 0, 0);
//    Assert.assertEquals(0, routeProgress.getFractionTraveled(), BaseTest.LARGE_DELTA);
//  }
//
//  @Test
//  public void multiLeg_getFractionTraveled_equalsCorrectValueAtIntervals() {
//    // Chop the line in small pieces
//    for (RouteLeg leg : multiLegRoute.getLegs()) {
//      for (int step = 0; step < leg.getSteps().size(); step++) {
//        Position position = multiLegRoute.getLegs().get(0).getSteps().get(step).getManeuver().asPosition();
//        RouteProgress routeProgress = RouteProgress.create(multiLegRoute, buildTestLocation(position), 0, step);
//        float fractionRemaining = (float) (routeProgress.getDistanceTraveled() / multiLegRoute.getDistance());
//        Assert.assertEquals(fractionRemaining, routeProgress.getFractionTraveled(), BaseTest.LARGE_DELTA);
//      }
//    }
//  }
//
//  @Test
//  public void multiLeg_getFractionTraveled_equalsOneAtEndOfRoute() {
//    Position lastCoordinate
//      = multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1)
//      .getSteps().get(multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1)
//        .getSteps().size() - 1).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(
//      multiLegRoute, buildTestLocation(lastCoordinate), multiLegRoute.getLegs().size() - 1,
//      multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1);
//    Assert.assertEquals(1.0, routeProgress.getFractionTraveled(), DELTA);
//  }
//
//  @Test
//  public void multiLeg_getDurationRemaining_equalsRouteDurationAtBeginning() {
//    Position firstCoordinate = multiLegRoute.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(multiLegRoute, buildTestLocation(firstCoordinate), 0, 0);
//    Assert.assertEquals(2858.1, routeProgress.getDurationRemaining(), BaseTest.LARGE_DELTA);
//  }
//
//  @Test
//  public void multiLeg_getDurationRemaining_equalsZeroAtEndOfRoute() {
//    Position lastCoordinate
//      = multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().get(multiLegRoute.getLegs()
//      .get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(multiLegRoute, buildTestLocation(lastCoordinate),
//      multiLegRoute.getLegs().size() - 1, multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1)
//        .getSteps().size() - 1);
//    Assert.assertEquals(0, routeProgress.getDurationRemaining(), BaseTest.DELTA);
//  }
//
//  @Test
//  public void multiLeg_getDistanceTraveled_equalsZeroAtBeginning() {
//    Position firstCoordinate = multiLegRoute.getLegs().get(0).getSteps().get(0).getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(multiLegRoute, buildTestLocation(firstCoordinate), 0, 0);
//    Assert.assertEquals(0, routeProgress.getDistanceTraveled(), BaseTest.LARGE_DELTA);
//  }
//
//  @Test
//  public void multiLeg_getDistanceTraveled_equalsRouteDistanceAtEndOfRoute() {
//    Position lastCoordinate
//      = multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1)
//      .getSteps().get(multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1).getSteps().size() - 1)
//      .getManeuver().asPosition();
//    RouteProgress routeProgress = RouteProgress.create(multiLegRoute, buildTestLocation(lastCoordinate),
//      multiLegRoute.getLegs().size() - 1, multiLegRoute.getLegs().get(multiLegRoute.getLegs().size() - 1)
//        .getSteps().size() - 1);
//    Assert.assertEquals(multiLegRoute.getDistance(), routeProgress.getDistanceTraveled(), BaseTest.DELTA);
//  }
//
//  @Test
//  public void multiLeg_getLegIndex_returnsCurrentLegIndex() {
//    RouteProgress routeProgress = RouteProgress.create(multiLegRoute, Mockito.mock(Location.class), 1, 0);
//    Assert.assertEquals(1, routeProgress.getLegIndex());
//  }
//
//  private Location buildTestLocation(Position position) {
//    Location location = new Location("test");
//    location.setLatitude(position.getLatitude());
//    location.setLongitude(position.getLongitude());
//    return location;
//  }
}
package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.directions.v5.DirectionsAdapterFactory;
import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.constants.Constants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class NavigationHelperTest extends BaseTest {

  // Fixtures
  private static final String MULTI_LEG_ROUTE = "directions_two_leg_route.json";

  private RouteProgress.Builder routeProgressBuilder;
  private DirectionsRoute route;

  @Before
  public void setUp() throws Exception {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(MULTI_LEG_ROUTE);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.routes().get(0);

    Location location = new Location("test");
    List<Point> coords = PolylineUtils.decode(route.legs().get(0).steps().get(1).geometry(),
      Constants.PRECISION_6);
    location.setLatitude(coords.get(0).latitude());
    location.setLongitude(coords.get(0).longitude());

    routeProgressBuilder = RouteProgress.builder()
      .directionsRoute(route)
      .distanceRemaining(1000)
      .stepDistanceRemaining(1000)
      .legDistanceRemaining(1000)
      .stepIndex(0)
      .legIndex(0);
  }

  @Test
  public void increaseIndex_increasesStepByOne() throws Exception {
    RouteProgress routeProgress = routeProgressBuilder.legIndex(0).stepIndex(0).build();
    NavigationIndices previousIndices = NavigationIndices.create(0, 0);
    NavigationIndices newIndices = NavigationHelper.increaseIndex(routeProgress, previousIndices);
    assertEquals(0, newIndices.legIndex());
    assertEquals(1, newIndices.stepIndex());
  }

  @Test
  public void increaseIndex_increasesLegIndex() throws Exception {
    RouteProgress routeProgress = routeProgressBuilder
      .legIndex(0)
      .stepIndex(21)
      .build();
    NavigationIndices previousIndices = NavigationIndices.create(0, 21);
    NavigationIndices newIndices = NavigationHelper.increaseIndex(routeProgress, previousIndices);
    assertEquals(1, newIndices.legIndex());
  }

  @Test
  public void increaseIndex_stepIndexResetsOnLegIndexIncrease() throws Exception {
    RouteProgress routeProgress = routeProgressBuilder
      .legIndex(0)
      .stepIndex(21)
      .build();
    NavigationIndices previousIndices = NavigationIndices.create(0, 21);
    NavigationIndices newIndices = NavigationHelper.increaseIndex(routeProgress, previousIndices);
    assertEquals(0, newIndices.stepIndex());
  }

  //  @Test
  //  public void checkMilestones_onlyTriggeredMilestonesGetReturned() throws Exception {
  //    RouteProgress routeProgress = routeProgressBuilder
  //      .legIndex(0)
  //      .stepIndex(0)
  //      .build();
  //    MapboxNavigationOptions options = MapboxNavigationOptions.builder()
  //      .defaultMilestonesEnabled(false).build();
  //    MapboxNavigation mapboxNavigation = new MapboxNavigation(
  //      RuntimeEnvironment.application.getApplicationContext(), "pk.XXX", options);
  //    mapboxNavigation.addMilestone(new StepMilestone.Builder()
  //      .setTrigger(Trigger.eq(TriggerProperty.STEP_INDEX, 0))
  //      .setIdentifier(1001).build());
  //    mapboxNavigation.addMilestone(new StepMilestone.Builder()
  //      .setTrigger(Trigger.eq(TriggerProperty.STEP_INDEX, 4))
  //      .setIdentifier(1002).build());
  //
  //    List<Milestone> triggeredMilestones
  //      = checkMilestones(routeProgress, routeProgress, mapboxNavigation);
  //    assertEquals(1, triggeredMilestones.size());
  //    assertEquals(1001, triggeredMilestones.get(0).getIdentifier());
  //    assertNotSame(1002, triggeredMilestones.get(0).getIdentifier());
  //  }

  @Test
  public void stepDistanceRemaining_returnsZeroWhenPositionsEqualEachOther() throws Exception {
    Point snappedPoint = Point.fromLngLat(-77.062996, 38.798405);
    List<Point> coordinates = PolylineUtils.decode(
      route.legs().get(0).steps().get(1).geometry(), Constants.PRECISION_6);
    double distance = NavigationHelper.stepDistanceRemaining(snappedPoint, 0,
      1, route, coordinates);
    assertEquals(0.0, distance);
  }

  @Test
  public void nextManeuverPosition_correctlyReturnsNextManeuverPosition() throws Exception {
    List<Point> coordinates = PolylineUtils.decode(
      route.legs().get(0).steps().get(0).geometry(), Constants.PRECISION_6);
    Point nextManeuver = NavigationHelper.nextManeuverPosition(0,
      route.legs().get(0).steps(), coordinates);
    assertTrue(nextManeuver.equals(route.legs().get(0).steps().get(1).maneuver().location()));
  }

  @Test
  public void nextManeuverPosition_correctlyReturnsNextManeuverPositionInNextLeg() throws Exception {
    int stepIndex = route.legs().get(0).steps().size() - 1;
    List<Point> coordinates = PolylineUtils.decode(
      route.legs().get(0).steps().get(stepIndex).geometry(), Constants.PRECISION_6);
    Point nextManeuver = NavigationHelper.nextManeuverPosition(stepIndex,
      route.legs().get(0).steps(), coordinates);
    assertTrue(nextManeuver.equals(route.legs().get(1).steps().get(0).maneuver().location()));
  }
}
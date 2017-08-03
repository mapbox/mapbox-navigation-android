package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import android.location.Location;

import com.google.gson.Gson;
import com.mapbox.services.Constants;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.StepMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Trigger;
import com.mapbox.services.android.navigation.v5.milestone.TriggerProperty;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;
import com.mapbox.services.commons.utils.PolylineUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.checkMilestones;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class NavigationHelperTest extends BaseTest {

  // Fixtures
  private static final String MULTI_LEG_ROUTE = "directions_two_leg_route.json";

  private RouteProgress.Builder routeProgressBuilder;
  private DirectionsRoute route;

  @Before
  public void setUp() throws Exception {
    Gson gson = new Gson();
    String body = readPath(MULTI_LEG_ROUTE);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.getRoutes().get(0);

    Location location = new Location("test");
    List<Position> coords = PolylineUtils.decode(route.getLegs().get(0).getSteps().get(1).getGeometry(),
      Constants.PRECISION_6);
    location.setLatitude(coords.get(0).getLatitude());
    location.setLongitude(coords.get(0).getLongitude());



    routeProgressBuilder = RouteProgress.builder()
      .directionsRoute(route)
      .distanceRemaining(1000)
      .stepDistanceRemaining(1000)
      .legDistanceRemaining(1000)
      .stepIndex(0)
      .legIndex(0)
      .location(location);
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

  @Test
  public void checkMilestones_onlyTriggeredMilestonesGetReturned() throws Exception {
    RouteProgress routeProgress = routeProgressBuilder
      .legIndex(0)
      .stepIndex(0)
      .build();
    MapboxNavigationOptions options = MapboxNavigationOptions.builder()
      .defaultMilestonesEnabled(false).build();
    MapboxNavigation mapboxNavigation = new MapboxNavigation(mock(Context.class), options);
    mapboxNavigation.addMilestone(new StepMilestone.Builder()
      .setTrigger(Trigger.eq(TriggerProperty.STEP_INDEX, 0))
      .setIdentifier(1001).build());
    mapboxNavigation.addMilestone(new StepMilestone.Builder()
      .setTrigger(Trigger.eq(TriggerProperty.STEP_INDEX, 4))
      .setIdentifier(1002).build());

    List<Milestone> triggeredMilestones
      = checkMilestones(routeProgress, routeProgress, mapboxNavigation);
    assertEquals(1, triggeredMilestones.size());
    assertEquals(1001, triggeredMilestones.get(0).getIdentifier());
    assertNotSame(1002, triggeredMilestones.get(0).getIdentifier());
  }







}
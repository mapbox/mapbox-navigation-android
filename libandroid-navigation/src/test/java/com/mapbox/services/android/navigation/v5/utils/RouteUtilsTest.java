package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgressState;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RouteUtilsTest extends BaseTest {

  @Test
  public void isArrivalEvent_returnsTrueWhenRouteProgressStateIsArrived() {
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.currentState()).thenReturn(RouteProgressState.ROUTE_ARRIVED);
    RouteUtils routeUtils = new RouteUtils();

    boolean isArrivalEvent = routeUtils.isArrivalEvent(routeProgress);

    assertTrue(isArrivalEvent);
  }

  @Test
  public void isArrivalEvent_returnsFalseWhenRouteProgressStateIsNotArrived() {
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.currentState()).thenReturn(RouteProgressState.LOCATION_TRACKING);
    RouteUtils routeUtils = new RouteUtils();

    boolean isArrivalEvent = routeUtils.isArrivalEvent(routeProgress);

    assertFalse(isArrivalEvent);
  }

  @Test
  public void findCurrentBannerInstructions_returnsNullWithNullCurrentStep() throws Exception {
    LegStep currentStep = null;
    double stepDistanceRemaining = 0;
    RouteUtils routeUtils = new RouteUtils();

    BannerInstructions currentBannerInstructions = routeUtils.findCurrentBannerInstructions(
      currentStep, stepDistanceRemaining
    );

    assertNull(currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerInstructions_returnsNullWithCurrentStepEmptyInstructions() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
    List<BannerInstructions> currentInstructions = currentStep.bannerInstructions();
    currentInstructions.clear();
    RouteUtils routeUtils = new RouteUtils();

    BannerInstructions currentBannerInstructions = routeUtils.findCurrentBannerInstructions(
      currentStep, stepDistanceRemaining
    );

    assertNull(currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerInstructions_returnsCorrectCurrentInstruction() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
    RouteUtils routeUtils = new RouteUtils();

    BannerInstructions currentBannerInstructions = routeUtils.findCurrentBannerInstructions(
      currentStep, stepDistanceRemaining
    );

    assertEquals(currentStep.bannerInstructions().get(0), currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerInstructions_adjustedDistanceRemainingReturnsCorrectInstruction() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = routeProgress.toBuilder()
      .stepDistanceRemaining(50)
      .build();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
    RouteUtils routeUtils = new RouteUtils();

    BannerInstructions currentBannerInstructions = routeUtils.findCurrentBannerInstructions(
      currentStep, stepDistanceRemaining
    );

    assertEquals(currentStep.bannerInstructions().get(0), currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerInstructions_adjustedDistanceRemainingRemovesCorrectInstructions() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = routeProgress.toBuilder()
      .stepIndex(1)
      .stepDistanceRemaining(500)
      .build();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
    RouteUtils routeUtils = new RouteUtils();

    BannerInstructions currentBannerInstructions = routeUtils.findCurrentBannerInstructions(
      currentStep, stepDistanceRemaining
    );

    assertEquals(currentStep.bannerInstructions().get(0), currentBannerInstructions);
  }

  @Test
  public void calculateRemainingWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypoints()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    List<Point> remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress);

    assertEquals(2, remainingWaypoints.size());
    assertEquals(Point.fromLngLat(7.890, 1.234), remainingWaypoints.get(0));
    assertEquals(Point.fromLngLat(5.678, 9.012), remainingWaypoints.get(1));
  }

  @Test
  public void calculateRemainingWaypoints_handlesNullOptions() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    when(route.routeOptions()).thenReturn(null);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypoints()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    List<Point> remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress);

    assertNull(remainingWaypoints);
  }

  @Test
  public void calculateRemainingWaypointNames() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointNames()).thenReturn("first;second;third;fourth");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypoints()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    String[] remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(routeProgress);

    assertEquals(3, remainingWaypointNames.length);
    assertEquals("first", remainingWaypointNames[0]);
    assertEquals("third", remainingWaypointNames[1]);
    assertEquals("fourth", remainingWaypointNames[2]);
  }

  @Test
  public void calculateRemainingWaypointNames_handlesNullOptions() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    when(route.routeOptions()).thenReturn(null);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypoints()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    String[] remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(routeProgress);

    assertNull(remainingWaypointNames);
  }

  private List<Point> buildCoordinateList() {
    List<Point> coordinates = new ArrayList<>();
    coordinates.add(Point.fromLngLat(1.234, 5.678));
    coordinates.add(Point.fromLngLat(9.012, 3.456));
    coordinates.add(Point.fromLngLat(7.890, 1.234));
    coordinates.add(Point.fromLngLat(5.678, 9.012));
    return coordinates;
  }
}
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
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
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
  public void findCurrentBannerInstructions_returnsNullWithNullCurrentStep() {
    LegStep currentStep = null;
    double stepDistanceRemaining = 0;
    RouteUtils routeUtils = new RouteUtils();

    BannerInstructions currentBannerInstructions = routeUtils.findCurrentBannerInstructions(
      currentStep, stepDistanceRemaining);

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
    List<BannerInstructions> bannerInstructions = currentStep.bannerInstructions();

    assertNotNull(bannerInstructions);
    assertEquals(bannerInstructions.get(0), currentBannerInstructions);
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
    List<BannerInstructions> bannerInstructions = currentStep.bannerInstructions();

    assertNotNull(bannerInstructions);
    assertEquals(bannerInstructions.get(0), currentBannerInstructions);
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
    List<BannerInstructions> bannerInstructions = currentStep.bannerInstructions();

    assertNotNull(bannerInstructions);
    assertEquals(bannerInstructions.get(0), currentBannerInstructions);
  }

  @Test
  public void calculateRemainingWaypoints_whenNoMiddleWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointIndices()).thenReturn("0;6");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(1);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    List<Point> remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress);

    Point[] expectedRemainingWaypoints = {Point.fromLngLat(4.56789, 0.12345)};

    assertNotNull(remainingWaypoints);
    assertArrayEquals(expectedRemainingWaypoints, remainingWaypoints.toArray());
  }

  @Test
  public void calculateRemainingWaypoints_whenOneMiddleWaypoint() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointIndices()).thenReturn("0;3;6");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    List<Point> remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress);

    Point[] expectedRemainingWaypoints = {Point.fromLngLat(7.89012, 3.45678),
      Point.fromLngLat(9.01234, 5.67890),
      Point.fromLngLat(2.34567, 8.90123),
      Point.fromLngLat(4.56789, 0.12345)};

    assertNotNull(remainingWaypoints);
    assertArrayEquals(expectedRemainingWaypoints, remainingWaypoints.toArray());
  }

  @Test
  public void calculateRemainingWaypoints_whenTwoMiddleWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointIndices()).thenReturn("0;2;4;6");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    List<Point> remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress);

    Point[] expectedRemainingWaypoints = {Point.fromLngLat(9.01234, 5.67890),
      Point.fromLngLat(2.34567, 8.90123),
      Point.fromLngLat(4.56789, 0.12345)};

    assertNotNull(remainingWaypoints);
    assertArrayEquals(expectedRemainingWaypoints, remainingWaypoints.toArray());
  }

  @Test
  public void calculateRemainingWaypoints_whenTwoMiddleOneByOneWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointIndices()).thenReturn("0;3;4;6");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(3);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    List<Point> remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress);

    Point[] expectedRemainingWaypoints = {Point.fromLngLat(7.89012, 3.45678),
      Point.fromLngLat(9.01234, 5.67890),
      Point.fromLngLat(2.34567, 8.90123),
      Point.fromLngLat(4.56789, 0.12345)};

    assertNotNull(remainingWaypoints);
    assertArrayEquals(expectedRemainingWaypoints, remainingWaypoints.toArray());
  }

  @Test
  public void calculateRemainingWaypoints_handlesNullOptions() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    when(route.routeOptions()).thenReturn(null);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    List<Point> remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress);

    assertNull(remainingWaypoints);
  }

  @Test
  public void calculateRemainingWaypointsIndices_whenNoMiddleWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointIndices()).thenReturn("0;6");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(1);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    Integer[] remainingWaypointsIndices = routeUtils.calculateRemainingWaypointIndices(routeProgress);

    Integer[] expectedRemainingWaypointsIndices = {0, 1};

    assertArrayEquals(expectedRemainingWaypointsIndices, remainingWaypointsIndices);
  }

  @Test
  public void calculateRemainingWaypointsIndices_whenOneMiddleWaypoint() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointIndices()).thenReturn("0;3;6");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    Integer[] remainingWaypointsIndices = routeUtils.calculateRemainingWaypointIndices(routeProgress);

    Integer[] expectedRemainingWaypointsIndices = {0, 1, 4};

    assertArrayEquals(expectedRemainingWaypointsIndices, remainingWaypointsIndices);
  }

  @Test
  public void calculateRemainingWaypointsIndices_whenTwoMiddleWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointIndices()).thenReturn("0;2;4;6");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    Integer[] remainingWaypointsIndices = routeUtils.calculateRemainingWaypointIndices(routeProgress);

    Integer[] expectedRemainingWaypointsIndices = {0, 1, 3};

    assertArrayEquals(expectedRemainingWaypointsIndices, remainingWaypointsIndices);
  }

  @Test
  public void calculateRemainingWaypointsIndices_whenTwoMiddleOneByOneWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointIndices()).thenReturn("0;3;4;6");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(3);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    Integer[] remainingWaypointsIndices = routeUtils.calculateRemainingWaypointIndices(routeProgress);

    Integer[] expectedRemainingWaypointsIndices = {0, 1, 2, 4};

    assertArrayEquals(expectedRemainingWaypointsIndices, remainingWaypointsIndices);
  }

  @Test
  public void calculateRemainingWaypointsIndices_handlesNullOptions() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    when(route.routeOptions()).thenReturn(null);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    Integer[] remainingWaypointsIndices = routeUtils.calculateRemainingWaypointIndices(routeProgress);

    assertNull(remainingWaypointsIndices);
  }

  @Test
  public void calculateRemainingWaypointNames_whenNoMiddleWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointNames()).thenReturn("first;seventh");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(1);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    String[] remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(routeProgress);

    String[] expectedRemainingWaypointNames = {"first", "seventh"};

    assertArrayEquals(expectedRemainingWaypointNames, remainingWaypointNames);
  }

  @Test
  public void calculateRemainingWaypointNames_whenOneMiddleWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointNames()).thenReturn("first;fourth;seventh");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    String[] remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(routeProgress);

    String[] expectedRemainingWaypointNames = {"first", "fourth", "seventh"};

    assertArrayEquals(expectedRemainingWaypointNames, remainingWaypointNames);
  }

  @Test
  public void calculateRemainingWaypointNames_whenTwoMiddleWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointNames()).thenReturn("first;third;fifth;seventh");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    String[] remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(routeProgress);

    String[] expectedRemainingWaypointNames = {"first", "fifth", "seventh"};

    assertArrayEquals(expectedRemainingWaypointNames, remainingWaypointNames);
  }

  @Test
  public void calculateRemainingWaypointNames_whenTwoMiddleOneByOneWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.waypointNames()).thenReturn("first;second;fifth;seventh");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(3);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    String[] remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(routeProgress);

    String[] expectedRemainingWaypointNames = {"first", "second", "fifth", "seventh"};

    assertArrayEquals(expectedRemainingWaypointNames, remainingWaypointNames);
  }

  @Test
  public void calculateRemainingWaypointNames_handlesNullOptions() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    when(route.routeOptions()).thenReturn(null);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    String[] remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(routeProgress);

    assertNull(remainingWaypointNames);
  }

  @Test
  public void calculateRemainingApproaches_whenNoMiddleWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.approaches()).thenReturn("curb;unrestricted");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(1);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    String[] remainingApproaches = routeUtils.calculateRemainingApproaches(routeProgress);

    String[] expectedRemainingApproaches = {"curb", "unrestricted"};

    assertArrayEquals(expectedRemainingApproaches, remainingApproaches);
  }

  @Test
  public void calculateRemainingApproaches_whenOneMiddleWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.approaches()).thenReturn("curb;curb;unrestricted");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    String[] remainingApproaches = routeUtils.calculateRemainingApproaches(routeProgress);

    String[] expectedRemainingApproaches = {"curb", "curb", "unrestricted"};

    assertArrayEquals(expectedRemainingApproaches, remainingApproaches);
  }

  @Test
  public void calculateRemainingApproaches_whenTwoMiddleWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.approaches()).thenReturn("curb;curb;curb;unrestricted");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    String[] remainingApproaches = routeUtils.calculateRemainingApproaches(routeProgress);

    String[] expectedRemainingApproaches = {"curb", "curb", "unrestricted"};

    assertArrayEquals(expectedRemainingApproaches, remainingApproaches);
  }

  @Test
  public void calculateRemainingApproaches_whenTwoMiddleOneByOneWaypoints() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    RouteOptions routeOptions = mock(RouteOptions.class);
    when(routeOptions.coordinates()).thenReturn(buildCoordinateList());
    when(routeOptions.approaches()).thenReturn("curb;curb;curb;unrestricted");
    when(route.routeOptions()).thenReturn(routeOptions);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(3);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    String[] remainingApproaches = routeUtils.calculateRemainingApproaches(routeProgress);

    String[] expectedRemainingApproaches = {"curb", "curb", "curb", "unrestricted"};

    assertArrayEquals(expectedRemainingApproaches, remainingApproaches);
  }

  @Test
  public void calculateRemainingApproaches_handlesNullOptions() {
    DirectionsRoute route = mock(DirectionsRoute.class);
    when(route.routeOptions()).thenReturn(null);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.remainingWaypointsCount()).thenReturn(2);
    when(routeProgress.directionsRoute()).thenReturn(route);
    RouteUtils routeUtils = new RouteUtils();

    String[] remainingApproaches = routeUtils.calculateRemainingApproaches(routeProgress);

    assertNull(remainingApproaches);
  }

  private List<Point> buildCoordinateList() {
    List<Point> coordinates = new ArrayList<>();
    coordinates.add(Point.fromLngLat(1.23456, 7.89012));
    coordinates.add(Point.fromLngLat(3.45678, 9.01234));
    coordinates.add(Point.fromLngLat(5.67890, 1.23456));
    coordinates.add(Point.fromLngLat(7.89012, 3.45678));
    coordinates.add(Point.fromLngLat(9.01234, 5.67890));
    coordinates.add(Point.fromLngLat(2.34567, 8.90123));
    coordinates.add(Point.fromLngLat(4.56789, 0.12345));
    return coordinates;
  }
}
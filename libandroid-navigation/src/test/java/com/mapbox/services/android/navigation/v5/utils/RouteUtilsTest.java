package com.mapbox.services.android.navigation.v5.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.directions.v5.DirectionsAdapterFactory;
import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class RouteUtilsTest extends BaseTest {

  private static final String PRECISION_6 = "directions_v5_precision_6.json";

  private RouteProgress routeProgress;
  private DirectionsRoute route;

  @Before
  public void setup() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    route = response.routes().get(0);
    routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(100)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();
  }

  @Test
  public void isNewRoute_returnsTrueWhenPreviousGeometriesNull() throws Exception {
    boolean isNewRoute = RouteUtils.isNewRoute(null, routeProgress);
    assertTrue(isNewRoute);
    RouteProgress previousRouteProgress = routeProgress;
    isNewRoute = RouteUtils.isNewRoute(previousRouteProgress, routeProgress);
    assertFalse(isNewRoute);
  }

  @Test
  public void isNewRoute_returnsFalseWhenGeometriesEqualEachOther() throws Exception {
    RouteProgress previousRouteProgress = routeProgress;
    boolean isNewRoute = RouteUtils.isNewRoute(previousRouteProgress, routeProgress);
    assertFalse(isNewRoute);
  }

  @Test
  public void isNewRoute_returnsTrueWhenGeometriesDoNotEqual() throws Exception {
    RouteProgress previousRouteProgress = RouteProgress.builder()
      .directionsRoute(route.toBuilder().geometry("vfejnqiv").build())
      .stepDistanceRemaining(100)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .stepIndex(0)
      .legIndex(0)
      .build();

    boolean isNewRoute = RouteUtils.isNewRoute(previousRouteProgress, routeProgress);
    assertTrue(isNewRoute);
  }

  @Test
  public void isDepartureEvent_returnsTrueWhenManeuverTypeDepart() throws Exception {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(100)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();

    boolean isDepartureEvent = RouteUtils.isDepartureEvent(routeProgress);
    assertTrue(isDepartureEvent);
  }

  @Test
  public void isDepartureEvent_returnsFalseWhenManeuverTypeIsNotDepart() throws Exception {
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(100)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(route)
      .stepIndex(1)
      .legIndex(0)
      .build();

    boolean isDepartureEvent = RouteUtils.isDepartureEvent(routeProgress);
    assertFalse(isDepartureEvent);
  }

  @Test
  public void isArrivalEvent_returnsTrueWhenManeuverTypeIsArrival_andIsValidMetersRemaining() throws Exception {
    RouteLeg lastLeg = route.legs().get(route.legs().size() - 1);
    int lastStepIndex = lastLeg.steps().indexOf(lastLeg.steps().get(lastLeg.steps().size() - 1));

    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(30)
      .legDistanceRemaining(30)
      .distanceRemaining(30)
      .directionsRoute(route)
      .stepIndex(lastStepIndex)
      .legIndex(0)
      .build();

    boolean isArrivalEvent = RouteUtils.isArrivalEvent(routeProgress);
    assertTrue(isArrivalEvent);
  }

  @Test
  public void isArrivalEvent_returnsFalseWhenManeuverTypeIsArrival_andIsNotValidMetersRemaining() throws Exception {
    RouteLeg lastLeg = route.legs().get(route.legs().size() - 1);
    int lastStepIndex = lastLeg.steps().indexOf(lastLeg.steps().get(lastLeg.steps().size() - 1));

    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(100)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(route)
      .stepIndex(lastStepIndex)
      .legIndex(0)
      .build();

    boolean isArrivalEvent = RouteUtils.isArrivalEvent(routeProgress);
    assertFalse(isArrivalEvent);
  }

  @Test
  public void isArrivalEvent_returnsFalseWhenManeuverTypeIsNotArrival_andIsValidMetersRemaining() throws Exception {
    RouteLeg lastLeg = route.legs().get(route.legs().size() - 1);
    int lastStepIndex = lastLeg.steps().indexOf(lastLeg.steps().get(lastLeg.steps().size() - 1));

    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(30)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(route)
      .stepIndex(lastStepIndex - 1)
      .legIndex(0)
      .build();

    boolean isArrivalEvent = RouteUtils.isArrivalEvent(routeProgress);
    assertFalse(isArrivalEvent);
  }

  @Test
  public void isArrivalEvent_returnsFalseWhenManeuverTypeIsNotArrival_andIsNotValidMetersRemaining() throws Exception {
    RouteLeg lastLeg = route.legs().get(route.legs().size() - 1);
    int lastStepIndex = lastLeg.steps().indexOf(lastLeg.steps().get(lastLeg.steps().size() - 1));

    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(200)
      .legDistanceRemaining(300)
      .distanceRemaining(300)
      .directionsRoute(route)
      .stepIndex(lastStepIndex - 1)
      .legIndex(0)
      .build();

    boolean isArrivalEvent = RouteUtils.isArrivalEvent(routeProgress);
    assertFalse(isArrivalEvent);
  }
}
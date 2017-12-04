package com.mapbox.services.android.navigation.v5.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class RouteUtilsTest extends BaseTest {

  private static final String PRECISION_6 = "directions_v5_precision_6.json";

  @Test
  public void isNewRoute_returnsTrueWhenPreviousGeometriesNull() throws Exception {
    RouteProgress defaultRouteProgress = obtainDefaultRouteProgress();
    boolean isNewRoute = RouteUtils.isNewRoute(null, defaultRouteProgress);
    assertTrue(isNewRoute);
    RouteProgress previousRouteProgress = obtainDefaultRouteProgress();

    isNewRoute = RouteUtils.isNewRoute(previousRouteProgress, defaultRouteProgress);

    assertFalse(isNewRoute);
  }

  @Test
  public void isNewRoute_returnsFalseWhenGeometriesEqualEachOther() throws Exception {
    RouteProgress previousRouteProgress = obtainDefaultRouteProgress();

    boolean isNewRoute = RouteUtils.isNewRoute(previousRouteProgress, previousRouteProgress);

    assertFalse(isNewRoute);
  }

  @Test
  public void isNewRoute_returnsTrueWhenGeometriesDoNotEqual() throws Exception {
    DirectionsRoute aRoute = obtainADirectionsRoute();
    RouteProgress defaultRouteProgress = obtainDefaultRouteProgress();
    RouteProgress previousRouteProgress = defaultRouteProgress.toBuilder()
      .directionsRoute(aRoute.toBuilder().geometry("vfejnqiv").build())
      .build();

    boolean isNewRoute = RouteUtils.isNewRoute(previousRouteProgress, defaultRouteProgress);

    assertTrue(isNewRoute);
  }

  @Test
  public void isArrivalEvent_returnsTrueWhenManeuverTypeIsArrival_andIsValidMetersRemaining() throws Exception {
    DirectionsRoute aRoute = obtainADirectionsRoute();
    int lastStepIndex = obtainLastStepIndex(aRoute);
    RouteProgress defaultRouteProgress = obtainDefaultRouteProgress();
    RouteProgress theRouteProgress = defaultRouteProgress.toBuilder()
      .stepDistanceRemaining(30)
      .legDistanceRemaining(30)
      .distanceRemaining(30)
      .stepIndex(lastStepIndex)
      .build();

    boolean isArrivalEvent = RouteUtils.isArrivalEvent(theRouteProgress);

    assertTrue(isArrivalEvent);
  }

  @Test
  public void isArrivalEvent_returnsFalseWhenManeuverTypeIsArrival_andIsNotValidMetersRemaining() throws Exception {
    DirectionsRoute aRoute = obtainADirectionsRoute();
    int lastStepIndex = obtainLastStepIndex(aRoute);
    RouteProgress defaultRouteProgress = obtainDefaultRouteProgress();
    RouteProgress theRouteProgress = defaultRouteProgress.toBuilder()
      .stepIndex(lastStepIndex)
      .legDistanceRemaining(100)
      .build();

    boolean isArrivalEvent = RouteUtils.isArrivalEvent(theRouteProgress);

    assertFalse(isArrivalEvent);
  }

  @Test
  public void isArrivalEvent_returnsTrueWhenUpcomingManeuverTypeIsArrival_andIsValidMetersRemaining() throws Exception {
    DirectionsRoute aRoute = obtainADirectionsRoute();
    int lastStepIndex = obtainLastStepIndex(aRoute);
    RouteProgress defaultRouteProgress = obtainDefaultRouteProgress();
    RouteProgress theRouteProgress = defaultRouteProgress.toBuilder()
      .legDistanceRemaining(30)
      .stepIndex(lastStepIndex - 1)
      .build();

    boolean isArrivalEvent = RouteUtils.isArrivalEvent(theRouteProgress);

    assertTrue(isArrivalEvent);
  }

  @Test
  public void isArrivalEvent_returnsFalseWhenManeuverTypeIsNotArrival_andIsNotValidMetersRemaining() throws Exception {
    DirectionsRoute aRoute = obtainADirectionsRoute();
    int lastStepIndex = obtainLastStepIndex(aRoute);
    RouteProgress defaultRouteProgress = obtainDefaultRouteProgress();
    RouteProgress theRouteProgress = defaultRouteProgress.toBuilder()
      .stepDistanceRemaining(200)
      .legDistanceRemaining(300)
      .distanceRemaining(300)
      .stepIndex(lastStepIndex - 1)
      .build();

    boolean isArrivalEvent = RouteUtils.isArrivalEvent(theRouteProgress);

    assertFalse(isArrivalEvent);
  }

  private RouteProgress obtainDefaultRouteProgress() throws Exception {
    DirectionsRoute aRoute = obtainADirectionsRoute();
    RouteProgress defaultRouteProgress = RouteProgress.builder()
      .stepDistanceRemaining(100)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(aRoute)
      .stepIndex(0)
      .legIndex(0)
      .build();

    return defaultRouteProgress;
  }

  private DirectionsRoute obtainADirectionsRoute() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    DirectionsRoute aRoute = response.routes().get(0);

    return aRoute;
  }

  private int obtainLastStepIndex(DirectionsRoute route) throws IOException {
    RouteLeg lastLeg = route.legs().get(route.legs().size() - 1);
    int lastStepIndex = lastLeg.steps().indexOf(lastLeg.steps().get(lastLeg.steps().size() - 1));

    return lastStepIndex;
  }
}
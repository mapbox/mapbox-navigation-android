package com.mapbox.services.android.navigation.v5.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.directions.v5.DirectionsAdapterFactory;
import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
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
}
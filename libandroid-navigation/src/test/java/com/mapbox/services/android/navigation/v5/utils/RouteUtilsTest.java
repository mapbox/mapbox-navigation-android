package com.mapbox.services.android.navigation.v5.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.directions.v5.DirectionsAdapterFactory;
import com.mapbox.directions.v5.models.DirectionsResponse;
import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.constants.Constants;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class RouteUtilsTest extends BaseTest {

  private static final String MULTI_LEG_ROUTE = "directions_two_leg_route.json";
  private static final String PRECISION_6 = "directions_v5_precision_6.json";

  private RouteProgress multiLegRouteProgress;
  private DirectionsRoute multiLegRoute;
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
      .currentStepCoordinates(PolylineUtils.decode(route.geometry(), Constants.PRECISION_6))
      .stepDistanceRemaining(100)
      .legDistanceRemaining(100)
      .distanceRemaining(100)
      .directionsRoute(route)
      .stepIndex(0)
      .legIndex(0)
      .build();

    body = loadJsonFixture(MULTI_LEG_ROUTE);
    response = gson.fromJson(body, DirectionsResponse.class);
    multiLegRoute = response.routes().get(0);
    multiLegRouteProgress = RouteProgress.builder()
      .currentStepCoordinates(PolylineUtils.decode(multiLegRoute.geometry(), Constants.PRECISION_6))
      .stepDistanceRemaining(1000)
      .legDistanceRemaining(1000)
      .distanceRemaining(1000)
      .directionsRoute(multiLegRoute)
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
    RouteProgress previousRouteProgress = routeProgress.toBuilder()
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
  public void increaseIndex_increasesStepByOne() throws Exception {
    RouteProgress newRouteProgress = RouteUtils.increaseIndex(multiLegRouteProgress);
    assertEquals(0, newRouteProgress.legIndex());
    assertEquals(1, newRouteProgress.currentLegProgress().stepIndex());
  }

  @Test
  public void increaseIndex_increasesLegIndex() throws Exception {
    RouteProgress routeProgress = multiLegRouteProgress.toBuilder()
      .legIndex(0)
      .stepIndex(21)
      .build();
    routeProgress = RouteUtils.increaseIndex(routeProgress);
    assertEquals(1, routeProgress.legIndex());
  }

  @Test
  public void increaseIndex_stepIndexResetsOnLegIndexIncrease() throws Exception {
    RouteProgress routeProgress = multiLegRouteProgress.toBuilder()
      .legIndex(0)
      .stepIndex(21)
      .build();
    routeProgress = RouteUtils.increaseIndex(routeProgress);
    assertEquals(0, routeProgress.currentLegProgress().stepIndex());
  }
}
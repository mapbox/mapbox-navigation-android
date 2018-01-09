package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;
import com.mapbox.services.android.navigation.v5.route.FasterRouteDetector;
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class FasterRouteDetectorTest extends BaseTest {

  private static final String PRECISION_6 = "directions_v5_precision_6.json";

  @Mock
  FasterRouteListener fasterRouteListener;

  private MapboxNavigation navigation;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    navigation = new MapboxNavigation(mock(Context.class), ACCESS_TOKEN, mock(NavigationTelemetry.class),
      mock(LocationEngine.class));
  }

  @Test
  public void sanity() throws Exception {
    FasterRouteDetector fasterRouteDetector = new FasterRouteDetector();
    assertNotNull(fasterRouteDetector);
  }

  @Test
  public void defaultFasterRouteEngine_didGetAddedOnInitialization() throws Exception {
    assertNotNull(navigation.getFasterRouteEngine());
  }

  @Test
  public void addFasterRouteEngine_didGetAdded() throws Exception {
    FasterRoute fasterRouteEngine = mock(FasterRoute.class);
    navigation.setFasterRouteEngine(fasterRouteEngine);
    assertEquals(navigation.getFasterRouteEngine(), fasterRouteEngine);
  }

  @Test
  public void onFasterRouteResponse_isFasterRouteIsTrue() throws Exception {
    navigation.addFasterRouteListener(fasterRouteListener);
    FasterRoute fasterRouteEngine = navigation.getFasterRouteEngine();

    // Create current progress
    RouteProgress currentProgress = obtainDefaultRouteProgress();
    DirectionsRoute longerRoute = currentProgress.directionsRoute().toBuilder()
      .duration(10000000d)
      .build();
    currentProgress = currentProgress.toBuilder()
      .directionsRoute(longerRoute)
      .build();

    // Create new direction response
    DirectionsResponse response = obtainADirectionsResponse();

    boolean isFasterRoute = fasterRouteEngine.isFasterRoute(response, currentProgress);
    assertTrue(isFasterRoute);
  }

  @Test
  public void onSlowerRouteResponse_isFasterRouteIsFalse() throws Exception {
    navigation.addFasterRouteListener(fasterRouteListener);
    FasterRoute fasterRouteEngine = navigation.getFasterRouteEngine();

    // Create current progress
    RouteProgress currentProgress = obtainDefaultRouteProgress();
    DirectionsRoute longerRoute = currentProgress.directionsRoute().toBuilder()
      .duration(1000d)
      .build();
    currentProgress = currentProgress.toBuilder()
      .directionsRoute(longerRoute)
      .build();

    // Create new direction response
    DirectionsResponse response = obtainADirectionsResponse();

    boolean isFasterRoute = fasterRouteEngine.isFasterRoute(response, currentProgress);
    assertFalse(isFasterRoute);
  }

  private RouteProgress obtainDefaultRouteProgress() throws Exception {
    DirectionsRoute aRoute = obtainADirectionsRoute();
    RouteProgress defaultRouteProgress = RouteProgress.builder()
      .stepDistanceRemaining(100)
      .legDistanceRemaining(700)
      .distanceRemaining(1000)
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

  private DirectionsResponse obtainADirectionsResponse() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    return response;
  }
}

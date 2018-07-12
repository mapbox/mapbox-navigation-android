package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import android.location.Location;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;
import com.mapbox.services.android.navigation.v5.route.FasterRouteDetector;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FasterRouteDetectorTest extends BaseTest {

  private static final String PRECISION_6 = "directions_v5_precision_6.json";

  @Test
  public void sanity() throws Exception {
    FasterRouteDetector fasterRouteDetector = new FasterRouteDetector();

    assertNotNull(fasterRouteDetector);
  }

  @Test
  public void defaultFasterRouteEngine_didGetAddedOnInitialization() throws Exception {
    MapboxNavigation navigation = buildNavigationWithFasterRouteEnabled();

    assertNotNull(navigation.getFasterRouteEngine());
  }

  @Test
  public void addFasterRouteEngine_didGetAdded() throws Exception {
    MapboxNavigation navigation = buildNavigationWithFasterRouteEnabled();
    FasterRoute fasterRouteEngine = mock(FasterRoute.class);

    navigation.setFasterRouteEngine(fasterRouteEngine);

    assertEquals(navigation.getFasterRouteEngine(), fasterRouteEngine);
  }

  @Test
  public void onFasterRouteResponse_isFasterRouteIsTrue() throws Exception {
    MapboxNavigation navigation = buildNavigationWithFasterRouteEnabled();
    FasterRoute fasterRouteEngine = navigation.getFasterRouteEngine();
    RouteProgress currentProgress = obtainDefaultRouteProgress();
    DirectionsRoute longerRoute = currentProgress.directionsRoute().toBuilder()
      .duration(10000000d)
      .build();
    currentProgress = currentProgress.toBuilder()
      .directionsRoute(longerRoute)
      .build();
    DirectionsResponse response = obtainADirectionsResponse();

    boolean isFasterRoute = fasterRouteEngine.isFasterRoute(response, currentProgress);

    assertTrue(isFasterRoute);
  }

  @Test
  public void onSlowerRouteResponse_isFasterRouteIsFalse() throws Exception {
    MapboxNavigation navigation = buildNavigationWithFasterRouteEnabled();
    FasterRoute fasterRouteEngine = navigation.getFasterRouteEngine();
    RouteProgress currentProgress = obtainDefaultRouteProgress();
    DirectionsRoute longerRoute = currentProgress.directionsRoute().toBuilder()
      .duration(1000d)
      .build();
    currentProgress = currentProgress.toBuilder()
      .directionsRoute(longerRoute)
      .build();
    DirectionsResponse response = obtainADirectionsResponse();

    boolean isFasterRoute = fasterRouteEngine.isFasterRoute(response, currentProgress);

    assertFalse(isFasterRoute);
  }

  @Test
  public void onNullLocationPassed_shouldCheckFasterRouteIsFalse() throws Exception {
    MapboxNavigation navigation = buildNavigationWithFasterRouteEnabled();
    FasterRoute fasterRouteEngine = navigation.getFasterRouteEngine();

    boolean checkFasterRoute = fasterRouteEngine.shouldCheckFasterRoute(null, obtainDefaultRouteProgress());

    assertFalse(checkFasterRoute);
  }

  @Test
  public void onNullRouteProgressPassed_shouldCheckFasterRouteIsFalse() throws Exception {
    MapboxNavigation navigation = buildNavigationWithFasterRouteEnabled();
    FasterRoute fasterRouteEngine = navigation.getFasterRouteEngine();

    boolean checkFasterRoute = fasterRouteEngine.shouldCheckFasterRoute(mock(Location.class), null);

    assertFalse(checkFasterRoute);
  }

  private MapboxNavigation buildNavigationWithFasterRouteEnabled() {
    MapboxNavigationOptions options = MapboxNavigationOptions.builder()
      .enableFasterRouteDetection(true)
      .build();
    Context context = mock(Context.class);
    when(context.getApplicationContext()).thenReturn(mock(Context.class));
    return new MapboxNavigation(context, ACCESS_TOKEN, options, mock(NavigationTelemetry.class),
      mock(LocationEngine.class));
  }

  private RouteProgress obtainDefaultRouteProgress() throws Exception {
    DirectionsRoute aRoute = obtainADirectionsRoute();
    return buildTestRouteProgress(aRoute, 100, 700, 1000, 0, 0);
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

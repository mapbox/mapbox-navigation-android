package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.DirectionsWaypoint;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationRouteEngine;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationRouteEngineCallback;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.DEFAULT_MANIFEST_NAME)
public class NavigationRouteEngineTest extends BaseTest {

  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  @Test
  public void sanity() throws Exception {
    NavigationRouteEngine routeEngine = new NavigationRouteEngine(mock(NavigationRouteEngineCallback.class), ACCESS_TOKEN);

    assertNotNull(routeEngine);
  }

  @Test
  public void onExtractOptionsWithRoute_routeUpdateCallbackIsCalled() throws Exception {
    NavigationRouteEngineCallback mockCallback = mock(NavigationRouteEngineCallback.class);
    NavigationRouteEngine routeEngine = new NavigationRouteEngine(mockCallback, ACCESS_TOKEN);
    NavigationViewOptions options = buildNavigationViewOptionsWithRoute();
    Context mockContext = mock(Context.class);
    DirectionsRoute directionsRoute = options.directionsRoute();

    routeEngine.extractRouteOptions(mockContext, options);

    verify(mockCallback).onRouteUpdate(directionsRoute);
  }

  @Test
  public void onExtractOptionsWithRoute_destinationCallbackIsCalled() throws Exception {
    NavigationRouteEngineCallback mockCallback = mock(NavigationRouteEngineCallback.class);
    NavigationRouteEngine routeEngine = new NavigationRouteEngine(mockCallback, ACCESS_TOKEN);
    NavigationViewOptions options = buildNavigationViewOptionsWithRoute();
    Point destination = findDestinationPoint(options);
    Context mockContext = mock(Context.class);

    routeEngine.extractRouteOptions(mockContext, options);

    verify(mockCallback).onDestinationSet(destination);
  }

  @Test
  public void onExtractOptionsWithCoordinates_destinationCallbackIsCalled() throws Exception {
    NavigationRouteEngineCallback mockCallback = mock(NavigationRouteEngineCallback.class);
    NavigationRouteEngine routeEngine = new NavigationRouteEngine(mockCallback, ACCESS_TOKEN);
    NavigationViewOptions options = buildNavigationViewOptionsWithCoordinates();
    Context mockContext = mock(Context.class);
    Point destination = options.destination();

    routeEngine.extractRouteOptions(mockContext, options);

    verify(mockCallback).onDestinationSet(destination);
  }

  private NavigationViewOptions buildNavigationViewOptionsWithRoute() throws IOException {
    MapboxNavigationOptions navigationOptions = MapboxNavigationOptions.builder().locale(Locale.US).build();
    return NavigationViewOptions.builder()
      .navigationOptions(navigationOptions)
      .directionsRoute(buildDirectionsRoute())
      .build();
  }

  private NavigationViewOptions buildNavigationViewOptionsWithCoordinates() throws IOException {
    MapboxNavigationOptions navigationOptions = MapboxNavigationOptions.builder().locale(Locale.US).build();
    return NavigationViewOptions.builder()
      .navigationOptions(navigationOptions)
      .origin(Point.fromLngLat(1.234455, 2.3456754))
      .destination(Point.fromLngLat(123.422, 346754.32244))
      .build();
  }

  private Point findDestinationPoint(NavigationViewOptions options) {
    List<Point> coordinates = options.directionsRoute().routeOptions().coordinates();
    return coordinates.get(coordinates.size() - 1);
  }

  private DirectionsRoute buildDirectionsRoute() throws IOException {
    Gson gson = new GsonBuilder()
      .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    RouteOptions options = buildRouteOptionsWithCoordinates(response);
    return response.routes().get(0).toBuilder().routeOptions(options).build();
  }

  private RouteOptions buildRouteOptionsWithCoordinates(DirectionsResponse response) {
    List<Point> coordinates = new ArrayList<>();
    for (DirectionsWaypoint waypoint : response.waypoints()) {
      coordinates.add(waypoint.location());
    }
    return RouteOptions.builder()
      .baseUrl("base_url")
      .user("user")
      .profile("profile")
      .accessToken(ACCESS_TOKEN)
      .requestUuid("uuid")
      .coordinates(coordinates).build();
  }
}

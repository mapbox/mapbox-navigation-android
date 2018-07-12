package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.DirectionsWaypoint;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.route.ViewRouteFetcher;
import com.mapbox.services.android.navigation.ui.v5.route.ViewRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ViewRouteFetcherTest extends BaseTest {

  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  @Test
  public void sanity() throws Exception {
    ViewRouteListener routeEngineListener = mock(ViewRouteListener.class);
    ViewRouteFetcher routeEngine = buildRouteEngine(routeEngineListener);

    assertNotNull(routeEngine);
  }

  @Test
  public void onExtractOptionsWithRoute_routeUpdateCallbackIsCalled() throws Exception {
    ViewRouteListener routeEngineListener = mock(ViewRouteListener.class);
    ViewRouteFetcher routeEngine = buildRouteEngine(routeEngineListener);
    NavigationViewOptions options = buildNavigationViewOptionsWithRoute();
    DirectionsRoute directionsRoute = options.directionsRoute();

    routeEngine.extractRouteOptions(options);

    verify(routeEngineListener).onRouteUpdate(directionsRoute);
  }

  @Test
  public void onExtractOptionsWithRoute_destinationCallbackIsCalled() throws Exception {
    ViewRouteListener routeEngineListener = mock(ViewRouteListener.class);
    ViewRouteFetcher routeEngine = buildRouteEngine(routeEngineListener);
    NavigationViewOptions options = buildNavigationViewOptionsWithRoute();
    Point destination = findDestinationPoint(options);

    routeEngine.extractRouteOptions(options);

    verify(routeEngineListener).onDestinationSet(destination);
  }

  @Test
  public void onRouteResponseReceived_routeUpdateCallbackIsCalled() throws Exception {
    ViewRouteListener routeEngineListener = mock(ViewRouteListener.class);
    ViewRouteFetcher routeEngine = buildRouteEngine(routeEngineListener);
    DirectionsResponse response = buildDirectionsResponse();
    DirectionsRoute route = response.routes().get(0);
    RouteProgress routeProgress = mock(RouteProgress.class);

    routeEngine.onResponseReceived(response, routeProgress);

    verify(routeEngineListener).onRouteUpdate(route);
  }

  @Test
  public void onErrorReceived_errorListenerIsTriggered() throws Exception {
    ViewRouteListener routeEngineListener = mock(ViewRouteListener.class);
    ViewRouteFetcher routeEngine = buildRouteEngine(routeEngineListener);
    Throwable throwable = mock(Throwable.class);

    routeEngine.onErrorReceived(throwable);

    verify(routeEngineListener).onRouteRequestError(throwable);
  }

  @NonNull
  private ViewRouteFetcher buildRouteEngine(ViewRouteListener routeEngineListener) {
    return new ViewRouteFetcher(mock(Context.class), ACCESS_TOKEN, routeEngineListener);
  }

  private NavigationViewOptions buildNavigationViewOptionsWithRoute() throws IOException {
    return NavigationViewOptions.builder()
      .directionsRoute(buildDirectionsRoute())
      .build();
  }

  private Point findDestinationPoint(NavigationViewOptions options) {
    List<Point> coordinates = options.directionsRoute().routeOptions().coordinates();
    return coordinates.get(coordinates.size() - 1);
  }

  private DirectionsRoute buildDirectionsRoute() throws IOException {
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(DIRECTIONS_PRECISION_6);
    DirectionsResponse response = gson.fromJson(body, DirectionsResponse.class);
    RouteOptions options = buildRouteOptionsWithCoordinates(response);
    return response.routes().get(0).toBuilder().routeOptions(options).build();
  }

  private DirectionsResponse buildDirectionsResponse() throws IOException {
    Gson gson = new GsonBuilder().registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
    String body = loadJsonFixture(DIRECTIONS_PRECISION_6);
    return gson.fromJson(body, DirectionsResponse.class);
  }

  private RouteOptions buildRouteOptionsWithCoordinates(DirectionsResponse response) {
    List<Point> coordinates = new ArrayList<>();
    for (DirectionsWaypoint waypoint : response.waypoints()) {
      coordinates.add(waypoint.location());
    }
    return RouteOptions.builder()
      .baseUrl(Constants.BASE_API_URL)
      .user("user")
      .profile("profile")
      .accessToken(ACCESS_TOKEN)
      .requestUuid("uuid")
      .geometries("mocked_geometries")
      .coordinates(coordinates).build();
  }
}

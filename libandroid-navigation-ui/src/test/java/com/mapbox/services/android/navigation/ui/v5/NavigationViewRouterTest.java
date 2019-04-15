package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;
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
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.route.RouteFetcher;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationViewRouterTest extends BaseTest {

  private static final String DIRECTIONS_PRECISION_6 = "directions_v5_precision_6.json";

  @Test
  public void sanity() {
    ViewRouteListener routeEngineListener = mock(ViewRouteListener.class);
    NavigationViewRouter routeEngine = buildRouteEngine(routeEngineListener);

    assertNotNull(routeEngine);
  }

  @Test
  public void onExtractOptionsWithRoute_routeUpdateCallbackIsCalled() throws Exception {
    ViewRouteListener routeEngineListener = mock(ViewRouteListener.class);
    NavigationViewRouter routeEngine = buildRouteEngine(routeEngineListener);
    NavigationViewOptions options = buildNavigationViewOptionsWithRoute();
    DirectionsRoute directionsRoute = options.directionsRoute();

    routeEngine.extractRouteOptions(options);

    verify(routeEngineListener).onRouteUpdate(directionsRoute);
  }

  @Test
  public void onExtractOptionsWithRoute_destinationCallbackIsCalled() throws Exception {
    ViewRouteListener routeEngineListener = mock(ViewRouteListener.class);
    NavigationViewRouter routeEngine = buildRouteEngine(routeEngineListener);
    NavigationViewOptions options = buildNavigationViewOptionsWithRoute();
    Point destination = findDestinationPoint(options);

    routeEngine.extractRouteOptions(options);

    verify(routeEngineListener).onDestinationSet(destination);
  }

  @Test
  public void onRouteResponseReceived_routeUpdateCallbackIsCalled() throws Exception {
    ViewRouteListener routeEngineListener = mock(ViewRouteListener.class);
    NavigationViewRouter routeEngine = buildRouteEngine(routeEngineListener);
    DirectionsResponse response = buildDirectionsResponse();
    DirectionsRoute route = response.routes().get(0);
    RouteProgress routeProgress = mock(RouteProgress.class);

    routeEngine.onResponseReceived(response, routeProgress);

    verify(routeEngineListener).onRouteUpdate(route);
  }

  @Test
  public void onErrorReceived_errorListenerIsTriggered() {
    ViewRouteListener routeEngineListener = mock(ViewRouteListener.class);
    NavigationViewRouter routeEngine = buildRouteEngine(routeEngineListener);
    Throwable throwable = mock(Throwable.class);
    when(throwable.getMessage()).thenReturn("error");

    routeEngine.onErrorReceived(throwable);

    verify(routeEngineListener).onRouteRequestError(eq("error"));
  }

  @Test
  public void findRouteFrom_fastConnectionGoesToOnline() {
    RouteFetcher onlineRouter = mock(RouteFetcher.class);
    NavigationRoute.Builder builder = mock(NavigationRoute.Builder.class);
    when(onlineRouter.buildRequestFrom(any(Location.class), any(RouteProgress.class))).thenReturn(builder);
    ConnectivityStatusProvider status = mock(ConnectivityStatusProvider.class);
    when(status.isConnectedFast()).thenReturn(true);
    NavigationViewRouter router = new NavigationViewRouter(
      onlineRouter,
      null, // Null offline (simulate no data)
      status,
      mock(RouteComparator.class),
      mock(ViewRouteListener.class),
      mock(RouteCallStatus.class)
    );
    router.updateLocation(mock(Location.class));

    router.findRouteFrom(mock(RouteProgress.class));

    verify(onlineRouter).findRouteWith(builder);
  }

  @Test
  public void findRouteFrom_nullOfflineAndSlowConnectionGoesToOnline() {
    RouteFetcher onlineRouter = mock(RouteFetcher.class);
    NavigationRoute.Builder builder = mock(NavigationRoute.Builder.class);
    when(onlineRouter.buildRequestFrom(any(Location.class), any(RouteProgress.class))).thenReturn(builder);
    ConnectivityStatusProvider status = mock(ConnectivityStatusProvider.class);
    when(status.isConnectedFast()).thenReturn(false);
    when(status.isConnected()).thenReturn(true);
    NavigationViewRouter router = new NavigationViewRouter(
      onlineRouter,
      null, // Null offline (simulate no data)
      status,
      mock(RouteComparator.class),
      mock(ViewRouteListener.class),
      mock(RouteCallStatus.class)
    );
    router.updateLocation(mock(Location.class));

    router.findRouteFrom(mock(RouteProgress.class));

    verify(onlineRouter).findRouteWith(builder);
  }

  @Test
  public void findRouteFrom_slowConnectionGoesToOffline() {
    RouteFetcher onlineRouter = mock(RouteFetcher.class);
    NavigationRoute.Builder builder = mock(NavigationRoute.Builder.class);
    when(onlineRouter.buildRequestFrom(any(Location.class), any(RouteProgress.class))).thenReturn(builder);
    ConnectivityStatusProvider status = mock(ConnectivityStatusProvider.class);
    when(status.isConnectedFast()).thenReturn(false);
    NavigationViewOfflineRouter offlineRouter = mock(NavigationViewOfflineRouter.class);
    when(offlineRouter.isConfigured()).thenReturn(true);
    NavigationViewRouter router = new NavigationViewRouter(
      onlineRouter,
      offlineRouter,
      status,
      mock(RouteComparator.class),
      mock(ViewRouteListener.class),
      mock(RouteCallStatus.class)
    );
    router.updateLocation(mock(Location.class));

    router.findRouteFrom(mock(RouteProgress.class));

    verify(offlineRouter).findRouteWith(builder);
  }

  @Test
  public void findRouteFrom_secondRequestIgnored() {
    RouteFetcher onlineRouter = mock(RouteFetcher.class);
    NavigationRoute.Builder builder = mock(NavigationRoute.Builder.class);
    when(onlineRouter.buildRequestFrom(any(Location.class), any(RouteProgress.class))).thenReturn(builder);
    ConnectivityStatusProvider status = mock(ConnectivityStatusProvider.class);
    when(status.isConnectedFast()).thenReturn(false);
    NavigationViewOfflineRouter offlineRouter = mock(NavigationViewOfflineRouter.class);
    when(offlineRouter.isConfigured()).thenReturn(true);
    NavigationViewRouter router = new NavigationViewRouter(
      onlineRouter,
      offlineRouter,
      status,
      mock(RouteComparator.class),
      mock(ViewRouteListener.class),
      mock(RouteCallStatus.class)
    );
    router.updateLocation(mock(Location.class));

    router.findRouteFrom(mock(RouteProgress.class));
    router.findRouteFrom(mock(RouteProgress.class));

    verify(offlineRouter, times(1)).findRouteWith(builder);
  }

  @Test
  public void onDestroy_clearsListeners() {
    RouteFetcher onlineRouter = mock(RouteFetcher.class);
    NavigationViewRouter router = new NavigationViewRouter(
      onlineRouter,
      mock(NavigationViewOfflineRouter.class),
      mock(ConnectivityStatusProvider.class),
      mock(RouteComparator.class),
      mock(ViewRouteListener.class),
      mock(RouteCallStatus.class)
    );

    router.onDestroy();

    verify(onlineRouter).cancelRouteCall();
  }

  @Test
  public void onDestroy_cancelsOnlineRouteCall() {
    RouteFetcher onlineRouter = mock(RouteFetcher.class);
    NavigationViewRouter router = new NavigationViewRouter(
      onlineRouter,
      mock(NavigationViewOfflineRouter.class),
      mock(ConnectivityStatusProvider.class),
      mock(RouteComparator.class),
      mock(ViewRouteListener.class),
      mock(RouteCallStatus.class)
    );

    router.onDestroy();

    verify(onlineRouter).clearListeners();
  }

  @Test
  public void checksOfflineRouterIsConfiguredIfPathAndTilesVersionArePresent() throws Exception {
    NavigationViewOfflineRouter offlineRouter = mock(NavigationViewOfflineRouter.class);
    String anOfflineRoutingTilesVersion = "offline_routing_tiles_version";
    NavigationViewOptions options = NavigationViewOptions.builder()
      .directionsRoute(buildDirectionsRoute())
      .offlineRoutingTilesPath("offline/routing/tiles/path")
      .offlineRoutingTilesVersion(anOfflineRoutingTilesVersion)
      .build();
    NavigationViewRouter router = new NavigationViewRouter(
      mock(RouteFetcher.class),
      offlineRouter,
      mock(ConnectivityStatusProvider.class),
      mock(RouteComparator.class),
      mock(ViewRouteListener.class),
      mock(RouteCallStatus.class)
    );

    router.extractRouteOptions(options);

    verify(offlineRouter).configure(eq(anOfflineRoutingTilesVersion));
  }

  @Test
  public void checksOfflineRouterIsNotConfiguredIfPathIsNull() throws Exception {
    NavigationViewOfflineRouter offlineRouter = mock(NavigationViewOfflineRouter.class);
    String anyOfflineRoutingTilesVersion = "any_offline_routing_tiles_version";
    NavigationViewOptions options = NavigationViewOptions.builder()
      .directionsRoute(buildDirectionsRoute())
      .offlineRoutingTilesPath(null)
      .offlineRoutingTilesVersion(anyOfflineRoutingTilesVersion)
      .build();
    NavigationViewRouter router = new NavigationViewRouter(
      mock(RouteFetcher.class),
      offlineRouter,
      mock(ConnectivityStatusProvider.class),
      mock(RouteComparator.class),
      mock(ViewRouteListener.class),
      mock(RouteCallStatus.class)
    );

    router.extractRouteOptions(options);

    verify(offlineRouter, times(0)).configure(eq(anyOfflineRoutingTilesVersion));
  }

  @Test
  public void checksOfflineRouterIsNotConfiguredIfPathIsEmpty() throws Exception {
    NavigationViewOfflineRouter offlineRouter = mock(NavigationViewOfflineRouter.class);
    String anyOfflineRoutingTilesVersion = "any_offline_routing_tiles_version";
    NavigationViewOptions options = NavigationViewOptions.builder()
      .directionsRoute(buildDirectionsRoute())
      .offlineRoutingTilesPath("")
      .offlineRoutingTilesVersion(anyOfflineRoutingTilesVersion)
      .build();
    NavigationViewRouter router = new NavigationViewRouter(
      mock(RouteFetcher.class),
      offlineRouter,
      mock(ConnectivityStatusProvider.class),
      mock(RouteComparator.class),
      mock(ViewRouteListener.class),
      mock(RouteCallStatus.class)
    );

    router.extractRouteOptions(options);

    verify(offlineRouter, times(0)).configure(eq(anyOfflineRoutingTilesVersion));
  }

  @Test
  public void checksOfflineRouterIsNotConfiguredIfTilesVersionIsNull() throws Exception {
    NavigationViewOfflineRouter offlineRouter = mock(NavigationViewOfflineRouter.class);
    String anyOfflinePath = "any/offline/routing/tiles/path";
    String nullOfflineRoutingTilesVersion = null;
    NavigationViewOptions options = NavigationViewOptions.builder()
      .directionsRoute(buildDirectionsRoute())
      .offlineRoutingTilesPath(null)
      .offlineRoutingTilesVersion(anyOfflinePath)
      .build();
    NavigationViewRouter router = new NavigationViewRouter(
      mock(RouteFetcher.class),
      offlineRouter,
      mock(ConnectivityStatusProvider.class),
      mock(RouteComparator.class),
      mock(ViewRouteListener.class),
      mock(RouteCallStatus.class)
    );

    router.extractRouteOptions(options);

    verify(offlineRouter, times(0)).configure(eq(nullOfflineRoutingTilesVersion));
  }

  @Test
  public void checksOfflineRouterIsNotConfiguredIfTilesVersionIsEmpty() throws Exception {
    NavigationViewOfflineRouter offlineRouter = mock(NavigationViewOfflineRouter.class);
    String anyOfflinePath = "any/offline/routing/tiles/path";
    String emptyOfflineRoutingTilesVersion = "";
    NavigationViewOptions options = NavigationViewOptions.builder()
      .directionsRoute(buildDirectionsRoute())
      .offlineRoutingTilesPath(anyOfflinePath)
      .offlineRoutingTilesVersion(emptyOfflineRoutingTilesVersion)
      .build();
    NavigationViewRouter router = new NavigationViewRouter(
      mock(RouteFetcher.class),
      offlineRouter,
      mock(ConnectivityStatusProvider.class),
      mock(RouteComparator.class),
      mock(ViewRouteListener.class),
      mock(RouteCallStatus.class)
    );

    router.extractRouteOptions(options);

    verify(offlineRouter, times(0)).configure(eq(emptyOfflineRoutingTilesVersion));
  }

  @NonNull
  private NavigationViewRouter buildRouteEngine(ViewRouteListener routeEngineListener) {
    return new NavigationViewRouter(mock(RouteFetcher.class), mock(ConnectivityStatusProvider.class),
      routeEngineListener);
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

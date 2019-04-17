package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.core.utils.TextUtils;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.MapboxOfflineRouter;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.route.RouteFetcher;
import com.mapbox.services.android.navigation.v5.route.RouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.Date;
import java.util.List;

class NavigationViewRouter implements RouteListener {

  private final RouteFetcher onlineRouter;
  private final ConnectivityStatusProvider connectivityStatus;
  private final RouteComparator routeComparator;
  private final ViewRouteListener listener;
  @Nullable
  private NavigationViewOfflineRouter offlineRouter;

  private RouteOptions routeOptions;
  private DirectionsRoute currentRoute;
  private Location location;
  private RouteCallStatus callStatus;

  NavigationViewRouter(RouteFetcher onlineRouter, ConnectivityStatusProvider connectivityStatus,
                       ViewRouteListener listener) {
    this.onlineRouter = onlineRouter;
    this.connectivityStatus = connectivityStatus;
    this.listener = listener;
    this.routeComparator = new RouteComparator(this);
    onlineRouter.addRouteListener(this);
  }

  // Extra fields for testing purposes
  NavigationViewRouter(RouteFetcher onlineRouter, NavigationViewOfflineRouter offlineRouter,
                       ConnectivityStatusProvider connectivityStatus, RouteComparator routeComparator,
                       ViewRouteListener listener, RouteCallStatus callStatus) {
    this.onlineRouter = onlineRouter;
    this.offlineRouter = offlineRouter;
    this.connectivityStatus = connectivityStatus;
    this.routeComparator = routeComparator;
    this.listener = listener;
    this.callStatus = callStatus;
    onlineRouter.addRouteListener(this);
  }

  @Override
  public void onResponseReceived(DirectionsResponse response, @Nullable RouteProgress routeProgress) {
    if (validRouteResponse(response)) {
      routeComparator.compare(response, currentRoute);
    }
    updateCallStatusReceived();
  }

  @Override
  public void onErrorReceived(Throwable throwable) {
    onRequestError(throwable.getMessage());
    updateCallStatusReceived();
  }

  void extractRouteOptions(NavigationViewOptions options) {
    extractRouteFrom(options);
    initializeOfflineFrom(options);
  }

  void findRouteFrom(@Nullable RouteProgress routeProgress) {
    if (isRouting()) {
      return;
    }
    NavigationRoute.Builder builder = onlineRouter.buildRequestFrom(location, routeProgress);
    if (connectivityStatus.isConnectedFast()) {
      findOnlineRouteWith(builder);
    } else if (isOfflineConfigured()) {
      findOfflineRouteWith(builder);
    } else if (connectivityStatus.isConnected()) {
      findOnlineRouteWith(builder);
    }
  }

  void updateLocation(@NonNull Location location) {
    this.location = location;
  }

  void updateCurrentRoute(DirectionsRoute currentRoute) {
    this.currentRoute = currentRoute;
    listener.onRouteUpdate(currentRoute);
  }

  void updateCallStatusReceived() {
    if (callStatus != null) {
      callStatus.setResponseReceived();
    }
  }

  void onRequestError(String errorMessage) {
    listener.onRouteRequestError(errorMessage);
  }

  void onDestroy() {
    onlineRouter.cancelRouteCall();
    onlineRouter.clearListeners();
  }

  private boolean validRouteResponse(DirectionsResponse response) {
    return response != null && !response.routes().isEmpty();
  }

  private void extractRouteFrom(NavigationViewOptions options) {
    DirectionsRoute route = options.directionsRoute();
    cacheRouteOptions(route.routeOptions());
    updateCurrentRoute(route);
  }

  private void cacheRouteOptions(RouteOptions routeOptions) {
    this.routeOptions = routeOptions;
    cacheRouteDestination();
  }

  private void cacheRouteDestination() {
    boolean hasValidCoordinates = routeOptions != null && !routeOptions.coordinates().isEmpty();
    if (hasValidCoordinates) {
      List<Point> coordinates = routeOptions.coordinates();
      int destinationCoordinate = coordinates.size() - 1;
      Point destinationPoint = coordinates.get(destinationCoordinate);
      listener.onDestinationSet(destinationPoint);
    }
  }

  private void initializeOfflineFrom(NavigationViewOptions options) {
    String offlinePath = options.offlineRoutingTilesPath();
    String offlineTilesVersion = options.offlineRoutingTilesVersion();
    if (!TextUtils.isEmpty(offlinePath) && !TextUtils.isEmpty(offlineTilesVersion)) {
      if (this.offlineRouter == null) {
        MapboxOfflineRouter offlineRouter = new MapboxOfflineRouter(offlinePath);
        this.offlineRouter = new NavigationViewOfflineRouter(offlineRouter, this);
      }
      this.offlineRouter.configure(offlineTilesVersion);
    }
  }

  private boolean isOfflineConfigured() {
    return offlineRouter != null && offlineRouter.isConfigured();
  }

  private void findOnlineRouteWith(NavigationRoute.Builder builder) {
    onlineRouter.cancelRouteCall();
    onlineRouter.findRouteWith(builder);
    callStatus = new RouteCallStatus(new Date());
  }

  private void findOfflineRouteWith(NavigationRoute.Builder builder) {
    offlineRouter.findRouteWith(builder);
    callStatus = new RouteCallStatus(new Date());
  }

  private boolean isRouting() {
    if (callStatus == null) {
      return false;
    }
    return callStatus.isRouting(new Date());
  }
}
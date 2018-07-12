package com.mapbox.services.android.navigation.v5.route;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * This class can be used to fetch new routes given a {@link Location} origin and
 * {@link RouteOptions} provided by a {@link RouteProgress}.
 */
public class RouteFetcher {

  private static final double BEARING_TOLERANCE = 90d;

  private final List<RouteListener> routeListeners = new CopyOnWriteArrayList<>();
  private final String accessToken;
  private final WeakReference<Context> contextWeakReference;

  private RouteProgress routeProgress;
  private RouteUtils routeUtils;

  public RouteFetcher(Context context, String accessToken) {
    this.accessToken = accessToken;
    contextWeakReference = new WeakReference<>(context);
    routeUtils = new RouteUtils();
  }

  public void addRouteListener(RouteListener listener) {
    if (!routeListeners.contains(listener)) {
      routeListeners.add(listener);
    }
  }

  public void clearListeners() {
    routeListeners.clear();
  }

  /**
   * Calculates a new {@link com.mapbox.api.directions.v5.models.DirectionsRoute} given
   * the current {@link Location} and {@link RouteProgress} along the route.
   * <p>
   * Uses {@link RouteOptions#coordinates()} and {@link RouteProgress#remainingWaypoints()}
   * to determine the amount of remaining waypoints there are along the given route.
   *
   * @param location      current location of the device
   * @param routeProgress for remaining waypoints along the route
   * @since 0.13.0
   */
  public void findRouteFromRouteProgress(Location location, RouteProgress routeProgress) {
    if (isInvalidProgress(location, routeProgress)) {
      return;
    }
    this.routeProgress = routeProgress;
    NavigationRoute.Builder builder = buildRequestFromLocation(location, routeProgress);
    executeRouteCall(builder);
  }

  @Nullable
  private NavigationRoute.Builder buildRequestFromLocation(Location location, RouteProgress progress) {
    Context context = contextWeakReference.get();
    if (context == null) {
      return null;
    }
    Point origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    Double bearing = location.hasBearing() ? Float.valueOf(location.getBearing()).doubleValue() : null;
    RouteOptions options = progress.directionsRoute().routeOptions();
    NavigationRoute.Builder builder = NavigationRoute.builder(context)
      .origin(origin, bearing, BEARING_TOLERANCE)
      .routeOptions(options);

    List<Point> remainingWaypoints = routeUtils.calculateRemainingWaypoints(progress);
    if (remainingWaypoints == null) {
      Timber.e("An error occurred fetching a new route");
      return null;
    }
    addDestination(remainingWaypoints, builder);
    addWaypoints(remainingWaypoints, builder);
    return builder;
  }

  private void addDestination(List<Point> remainingWaypoints, NavigationRoute.Builder builder) {
    if (!remainingWaypoints.isEmpty()) {
      builder.destination(retrieveDestinationWaypoint(remainingWaypoints));
    }
  }

  private Point retrieveDestinationWaypoint(List<Point> remainingWaypoints) {
    int lastWaypoint = remainingWaypoints.size() - 1;
    return remainingWaypoints.remove(lastWaypoint);
  }

  private void addWaypoints(List<Point> remainingCoordinates, NavigationRoute.Builder builder) {
    if (!remainingCoordinates.isEmpty()) {
      for (Point coordinate : remainingCoordinates) {
        builder.addWaypoint(coordinate);
      }
    }
  }

  private void executeRouteCall(NavigationRoute.Builder builder) {
    if (builder != null) {
      builder.accessToken(accessToken);
      builder.build().getRoute(directionsResponseCallback);
    }
  }

  private boolean isInvalidProgress(Location location, RouteProgress routeProgress) {
    return location == null || routeProgress == null;
  }

  private Callback<DirectionsResponse> directionsResponseCallback = new Callback<DirectionsResponse>() {
    @Override
    public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
      if (!response.isSuccessful()) {
        return;
      }
      updateListeners(response.body(), routeProgress);
    }

    @Override
    public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
      updateListenersWithError(throwable);
    }
  };

  private void updateListeners(DirectionsResponse response, RouteProgress routeProgress) {
    for (RouteListener listener : routeListeners) {
      listener.onResponseReceived(response, routeProgress);
    }
  }

  private void updateListenersWithError(Throwable throwable) {
    for (RouteListener listener : routeListeners) {
      listener.onErrorReceived(throwable);
    }
  }
}

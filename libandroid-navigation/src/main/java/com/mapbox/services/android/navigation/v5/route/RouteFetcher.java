package com.mapbox.services.android.navigation.v5.route;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.core.utils.TextUtils;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

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

  private List<RouteListener> routeListeners = new CopyOnWriteArrayList<>();

  private String accessToken;
  private String routeProfile;
  private RouteProgress routeProgress;
  private RouteUtils routeUtils;

  public RouteFetcher() {
    routeUtils = new RouteUtils();
  }

  public void addRouteListener(RouteListener listener) {
    if (!routeListeners.contains(listener)) {
      routeListeners.add(listener);
    }
  }

  public void removeRouteEngineListener(RouteListener listener) {
    routeListeners.remove(listener);
  }

  public void updateAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public void updateRouteProfile(String routeProfile) {
    if (routeUtils.isValidRouteProfile(routeProfile)) {
      this.routeProfile = routeProfile;
    }
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
  public void findRouteFromRouteProgress(Context context, Location location, RouteProgress routeProgress) {
    if (isValidProgress(location, routeProgress)) {
      return;
    }
    this.routeProgress = routeProgress;
    NavigationRoute.Builder builder = buildRouteRequest(context, location, routeProgress);
    executeRouteCall(builder);
  }

  @Nullable
  private NavigationRoute.Builder buildRouteRequestFromCurrentLocation(Context context, Point origin,
                                                                       Double bearing, RouteProgress progress,
                                                                       @Nullable String routeProfile) {
    RouteOptions options = progress.directionsRoute().routeOptions();
    NavigationRoute.Builder builder = NavigationRoute.builder(context)
      .origin(origin, bearing, BEARING_TOLERANCE)
      .routeOptions(options);

    List<Point> remainingWaypoints = routeUtils.calculateRemainingWaypoints(progress);
    if (remainingWaypoints == null) {
      Timber.e("An error occurred fetching a new route");
      return null;
    }
    addRouteProfile(routeProfile, builder);
    addDestination(remainingWaypoints, builder);
    addWaypoints(remainingWaypoints, builder);
    return builder;
  }

  private void addLanguage(NavigationRoute.Builder builder) {
    builder.language(routeProgress.directionsRoute().voiceLanguage());
  }

  private void addUnitType(NavigationRoute.Builder builder) {
    builder.voiceUnits(routeProgress.directionsRoute().routeOptions().voiceUnits());
  }

  private void addRouteProfile(String routeProfile, NavigationRoute.Builder builder) {
    if (!TextUtils.isEmpty(routeProfile)) {
      builder.profile(routeProfile);
    }
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

  private boolean isValidProgress(Location location, RouteProgress routeProgress) {
    return location == null || routeProgress == null;
  }

  private NavigationRoute.Builder buildRouteRequest(Context context, Location location, RouteProgress routeProgress) {
    Point origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    Double bearing = location.hasBearing() ? Float.valueOf(location.getBearing()).doubleValue() : null;
    return buildRouteRequestFromCurrentLocation(
      context, origin, bearing, routeProgress, routeProfile
    );
  }

  private void executeRouteCall(NavigationRoute.Builder builder) {
    if (builder != null) {
      builder.accessToken(accessToken);
      addLanguage(builder);
      addUnitType(builder);
      builder.build().getRoute(directionsResponseCallback);
    }
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

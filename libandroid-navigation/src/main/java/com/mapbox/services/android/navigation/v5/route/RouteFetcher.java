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

import java.lang.ref.WeakReference;
import java.util.Arrays;
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
  private static final String SEMICOLON = ";";
  private static final int ORIGIN_APPROACH_THRESHOLD = 1;
  private static final int ORIGIN_APPROACH = 0;
  private static final int FIRST_POSITION = 0;
  private static final int SECOND_POSITION = 1;

  private final List<RouteListener> routeListeners = new CopyOnWriteArrayList<>();
  private final String accessToken;
  private final WeakReference<Context> contextWeakReference;

  private NavigationRoute navigationRoute;
  private RouteProgress routeProgress;
  private RouteUtils routeUtils;

  public RouteFetcher(Context context, String accessToken) {
    this.accessToken = accessToken;
    contextWeakReference = new WeakReference<>(context);
    routeUtils = new RouteUtils();
  }

  // Package private (no modifier) for testing purposes
  RouteFetcher(Context context, String accessToken, NavigationRoute navigationRoute) {
    this.contextWeakReference = new WeakReference<>(context);
    this.navigationRoute = navigationRoute;
    this.accessToken = accessToken;
  }

  // Package private (no modifier) for testing purposes
  RouteFetcher(Context context, String accessToken, RouteUtils routeUtils) {
    this.contextWeakReference = new WeakReference<>(context);
    this.accessToken = accessToken;
    this.routeUtils = routeUtils;
  }

  /**
   * Adds a {@link RouteListener} to this class to be triggered when a route
   * response has been received.
   *
   * @param listener to be added
   */
  public void addRouteListener(RouteListener listener) {
    if (!routeListeners.contains(listener)) {
      routeListeners.add(listener);
    }
  }

  /**
   * Clears any listeners that have been added to this class via
   * {@link RouteFetcher#addRouteListener(RouteListener)}.
   */
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
    this.routeProgress = routeProgress;
    NavigationRoute.Builder builder = buildRequestFrom(location, routeProgress);
    findRouteWith(builder);
  }

  /**
   * Build a route request given the passed {@link Location} and {@link RouteProgress}.
   * <p>
   * Uses {@link RouteOptions#coordinates()} and {@link RouteProgress#remainingWaypoints()}
   * to determine the amount of remaining waypoints there are along the given route.
   *
   * @param location      current location of the device
   * @param routeProgress for remaining waypoints along the route
   * @return request reflecting the current progress
   */
  @Nullable
  public NavigationRoute.Builder buildRequestFrom(Location location, RouteProgress routeProgress) {
    Context context = contextWeakReference.get();
    if (invalid(context, location, routeProgress)) {
      return null;
    }
    Point origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    Double bearing = location.hasBearing() ? Float.valueOf(location.getBearing()).doubleValue() : null;
    RouteOptions options = routeProgress.directionsRoute().routeOptions();
    NavigationRoute.Builder builder = NavigationRoute.builder(context)
      .accessToken(accessToken)
      .origin(origin, bearing, BEARING_TOLERANCE)
      .routeOptions(options);

    List<Point> remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress);
    if (remainingWaypoints == null) {
      Timber.e("An error occurred fetching a new route");
      return null;
    }
    addDestination(remainingWaypoints, builder);
    addWaypoints(remainingWaypoints, builder);
    addWaypointNames(routeProgress, builder);
    addApproaches(routeProgress, builder);
    return builder;
  }

  /**
   * Executes the given NavigationRoute builder, eventually triggering
   * any {@link RouteListener} that has been added via {@link RouteFetcher#addRouteListener(RouteListener)}.
   *
   * @param builder to be executed
   */
  public void findRouteWith(NavigationRoute.Builder builder) {
    if (builder != null) {
      navigationRoute = builder.build();
      navigationRoute.getRoute(directionsResponseCallback);
    }
  }

  /**
   * Cancels the Directions API call if it has not been executed yet.
   */
  public void cancelRouteCall() {
    if (navigationRoute != null) {
      navigationRoute.cancelCall();
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

  private void addWaypointNames(RouteProgress progress, NavigationRoute.Builder builder) {
    String[] remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(progress);
    if (remainingWaypointNames != null) {
      builder.addWaypointNames(remainingWaypointNames);
    }
  }

  private void addApproaches(RouteProgress progress, NavigationRoute.Builder builder) {
    String[] remainingApproaches = calculateRemainingApproaches(progress);
    if (remainingApproaches != null) {
      builder.addApproaches(remainingApproaches);
    }
  }

  private String[] calculateRemainingApproaches(RouteProgress routeProgress) {
    RouteOptions routeOptions = routeProgress.directionsRoute().routeOptions();
    if (routeOptions == null || TextUtils.isEmpty(routeOptions.approaches())) {
      return null;
    }
    String allApproaches = routeOptions.approaches();
    String[] splitApproaches = allApproaches.split(SEMICOLON);
    int coordinatesSize = routeProgress.directionsRoute().routeOptions().coordinates().size();
    String[] remainingApproaches = Arrays.copyOfRange(splitApproaches,
      coordinatesSize - routeProgress.remainingWaypoints(), coordinatesSize);
    String[] approaches = new String[remainingApproaches.length + ORIGIN_APPROACH_THRESHOLD];
    approaches[ORIGIN_APPROACH] = splitApproaches[ORIGIN_APPROACH];
    System.arraycopy(remainingApproaches, FIRST_POSITION, approaches, SECOND_POSITION, remainingApproaches.length);
    return approaches;
  }

  private boolean invalid(Context context, Location location, RouteProgress routeProgress) {
    return context == null || location == null || routeProgress == null;
  }

  private Callback<DirectionsResponse> directionsResponseCallback = new Callback<DirectionsResponse>() {
    @Override
    public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
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

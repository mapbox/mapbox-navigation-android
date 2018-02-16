package com.mapbox.services.android.navigation.v5.route;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * This class can be used to fetch new routes given a {@link Point} origin and
 * {@link RouteOptions} provided by a {@link RouteProgress}.
 */
public class RouteEngine implements Callback<DirectionsResponse> {

  private static final double BEARING_TOLERANCE = 90d;

  private Callback engineCallback;
  private RouteProgress routeProgress;

  public RouteEngine(Callback engineCallback) {
    this.engineCallback = engineCallback;
  }

  public void fetchRoute(Point origin, RouteProgress routeProgress) {
    if (routeProgress == null) {
      return;
    }
    this.routeProgress = routeProgress;
    NavigationRoute.Builder builder = buildRouteRequestFromCurrentLocation(origin, null, routeProgress);
    if (builder != null) {
      builder.build().getRoute(this);
    }
  }

  @Override
  public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
    // Check for successful response
    if (!response.isSuccessful()) {
      return;
    }
    engineCallback.onResponseReceived(response, routeProgress);
  }

  @Override
  public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
    // No-op - fail silently
  }

  public interface Callback {
    void onResponseReceived(Response<DirectionsResponse> response, RouteProgress routeProgress);
  }

  @Nullable
  public static NavigationRoute.Builder buildRouteRequestFromCurrentLocation(Point origin, Double bearing,
                                                                             RouteProgress progress) {
    RouteOptions options = progress.directionsRoute().routeOptions();
    NavigationRoute.Builder builder = NavigationRoute.builder()
      .origin(origin, bearing, BEARING_TOLERANCE)
      .routeOptions(options);

    List<Point> remainingWaypoints = RouteUtils.calculateRemainingWaypoints(progress);
    if (remainingWaypoints == null) {
      Timber.e("An error occurred fetching a new route");
      return null;
    }
    addDestination(remainingWaypoints, builder);
    addWaypoints(remainingWaypoints, builder);
    return builder;
  }

  private static void addDestination(List<Point> remainingWaypoints, NavigationRoute.Builder builder) {
    if (!remainingWaypoints.isEmpty()) {
      builder.destination(retrieveDestinationWaypoint(remainingWaypoints));
    }
  }

  private static Point retrieveDestinationWaypoint(List<Point> remainingWaypoints) {
    int lastWaypoint = remainingWaypoints.size() - 1;
    return remainingWaypoints.remove(lastWaypoint);
  }

  private static void addWaypoints(List<Point> remainingCoordinates, NavigationRoute.Builder builder) {
    if (!remainingCoordinates.isEmpty()) {
      for (Point coordinate : remainingCoordinates) {
        builder.addWaypoint(coordinate);
      }
    }
  }
}

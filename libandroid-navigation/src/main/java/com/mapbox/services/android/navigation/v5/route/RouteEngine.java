package com.mapbox.services.android.navigation.v5.route;

import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class can be used to fetch new routes given a {@link Point} origin and
 * {@link RouteOptions} provided by a {@link RouteProgress}.
 */
public class RouteEngine implements Callback<DirectionsResponse> {

  private Callback engineCallback;
  private RouteProgress routeProgress;

  public RouteEngine(Callback engineCallback) {
    this.engineCallback = engineCallback;
  }

  public void fetchRoute(Point origin, RouteProgress routeProgress) {
    if (routeProgress == null) {
      return;
    } else {
      this.routeProgress = routeProgress;
    }

    // Calculate remaining waypoints
    List<Point> coordinates = routeProgress.directionsRoute().routeOptions().coordinates();
    List<Point> remainingCoordinates = coordinates.subList(0, routeProgress.remainingWaypoints());
    Point destination = remainingCoordinates.remove(remainingCoordinates.size() - 1);

    // Build new route request with current route options
    RouteOptions currentOptions = routeProgress.directionsRoute().routeOptions();
    NavigationRoute.Builder builder = NavigationRoute.builder()
      .origin(origin)
      .routeOptions(currentOptions);

    // Add waypoints with the remaining coordinate values
    addWaypoints(remainingCoordinates, builder);

    builder.destination(destination);
    builder.build().getRoute(this);
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

  private void addWaypoints(List<Point> remainingCoordinates, NavigationRoute.Builder builder) {
    if (!remainingCoordinates.isEmpty()) {
      for (Point coordinate : remainingCoordinates) {
        builder.addWaypoint(coordinate);
      }
    }
  }
}

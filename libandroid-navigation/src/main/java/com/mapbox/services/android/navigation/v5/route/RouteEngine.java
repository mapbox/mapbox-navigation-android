package com.mapbox.services.android.navigation.v5.route;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class can be used to fetch new routes given a {@link Location} origin and
 * {@link RouteOptions} provided by a {@link RouteProgress}.
 */
public class RouteEngine implements Callback<DirectionsResponse> {

  private RouteProgress routeProgress;
  private final Callback engineCallback;
  private final Locale locale;

  public RouteEngine(Locale locale, Callback engineCallback) {
    this.engineCallback = engineCallback;
    this.locale = locale;
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
   * @since 0.10.0
   */
  public void fetchRoute(Location location, RouteProgress routeProgress) {
    if (routeProgress == null) {
      return;
    }
    this.routeProgress = routeProgress;

    // Get the bearing from the location provided
    Double bearing = location.hasBearing() ? Float.valueOf(location.getBearing()).doubleValue() : null;
    // Convert the location to point for the builder
    Point origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());

    // Calculate remaining waypoints
    List<Point> coordinates = new ArrayList<>(routeProgress.directionsRoute().routeOptions().coordinates());

    if (coordinates.size() < routeProgress.remainingWaypoints()) {
      return;
    }
    // Remove any waypoints that have been passed
    coordinates.subList(0, routeProgress.remainingWaypoints()).clear();
    // Get the destination waypoint (last in the list)
    Point destination = coordinates.remove(coordinates.size() - 1);

    // Build new route request with the given origin and current route options
    RouteOptions currentOptions = routeProgress.directionsRoute().routeOptions();
    NavigationRoute.Builder builder = NavigationRoute.builder()
      .language(locale)
      .origin(origin, bearing, 90d)
      .routeOptions(currentOptions);

    // Add waypoints with the remaining coordinate values
    addWaypoints(coordinates, builder);

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
    engineCallback.onErrorReceived(throwable);
  }

  /**
   * Callback to be passed into the constructor of {@link RouteEngine}.
   * <p>
   * Will fire when either a successful / failed response is received.
   */
  public interface Callback {
    void onResponseReceived(Response<DirectionsResponse> response, RouteProgress routeProgress);

    void onErrorReceived(Throwable throwable);
  }

  private void addWaypoints(List<Point> remainingCoordinates, NavigationRoute.Builder builder) {
    if (!remainingCoordinates.isEmpty()) {
      for (Point coordinate : remainingCoordinates) {
        builder.addWaypoint(coordinate);
      }
    }
  }
}

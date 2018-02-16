package com.mapbox.services.android.navigation.v5.route;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * This class can be used to fetch new routes given a {@link Location} origin and
 * {@link RouteOptions} provided by a {@link RouteProgress}.
 */
public class RouteEngine implements Callback<DirectionsResponse> {
  private static final double BEARING_TOLERANCE = 90d;
  private RouteProgress routeProgress;
  private final Callback engineCallback;
  private final Locale locale;
  private final String unitType;

  public RouteEngine(Locale locale, @NavigationUnitType.UnitType int unitType, Callback engineCallback) {
    this.engineCallback = engineCallback;
    this.locale = locale;
    this.unitType = NavigationUnitType.getDirectionsCriteriaUnitType(unitType);
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
    if (location == null || routeProgress == null) {
      return;
    }
    this.routeProgress = routeProgress;

    Point origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    Double bearing = location.hasBearing() ? Float.valueOf(location.getBearing()).doubleValue() : null;
    NavigationRoute.Builder builder = buildRouteRequestFromCurrentLocation(origin, bearing, routeProgress);
    if (builder != null) {
      builder
        .language(locale)
        .voiceUnits(unitType);
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

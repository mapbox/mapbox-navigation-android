package com.mapbox.services.android.navigation.v5.route;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.core.utils.TextUtils;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * This class can be used to fetch new routes given a {@link Location} origin and
 * {@link RouteOptions} provided by a {@link RouteProgress}.
 */
public class NavigationRouteEngine implements RouteEngine {

  private static final double BEARING_TOLERANCE = 90d;

  private List<RouteEngineListener> routeEngineListeners = new CopyOnWriteArrayList<>();

  private String accessToken;
  private String routeProfile;
  private RouteProgress routeProgress;
  private Locale locale = Locale.getDefault();
  private int unitType = NavigationUnitType.NONE_SPECIFIED;

  @Override
  public void addRouteEngineListener(RouteEngineListener listener) {
    if (!routeEngineListeners.contains(listener)) {
      routeEngineListeners.remove(listener);
    }
  }

  @Override
  public void removeRouteEngineListener(RouteEngineListener listener) {
    routeEngineListeners.remove(listener);
  }

  @Override
  public void updateAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  @Override
  public void updateLocale(Locale locale) {
    this.locale = locale;
  }

  @Override
  public void updateUnitType(int unitType) {
    this.unitType = unitType;
  }

  @Override
  public void updateRouteProfile(String routeProfile) {
    if (RouteUtils.isValidRouteProfile(routeProfile)) {
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
  @Override
  public void findRouteFromRouteProgress(Location location, RouteProgress routeProgress) {
    if (isValidProgress(location, routeProgress)) {
      return;
    }
    this.routeProgress = routeProgress;
    Point origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    Double bearing = location.hasBearing() ? Float.valueOf(location.getBearing()).doubleValue() : null;
    NavigationRoute.Builder builder = buildRouteRequestFromCurrentLocation(
      origin, bearing, routeProgress, routeProfile
    );
    if (builder != null) {
      builder.accessToken(accessToken);
      addLocaleAndUnitType(builder);
      builder.build().getRoute(directionsResponseCallback);
    }
  }

  @Override
  public void findRouteFromOriginToDestination(Point origin, Point destination) {
    NavigationRoute.Builder builder = NavigationRoute.builder()
      .accessToken(accessToken)
      .origin(origin)
      .destination(destination);
    addLocaleAndUnitType(builder);
    addRouteProfile(routeProfile, builder);
    builder.build().getRoute(directionsResponseCallback);
  }

  @Nullable
  private static NavigationRoute.Builder buildRouteRequestFromCurrentLocation(Point origin, Double bearing,
                                                                              RouteProgress progress,
                                                                              @Nullable String routeProfile) {
    RouteOptions options = progress.directionsRoute().routeOptions();
    NavigationRoute.Builder builder = NavigationRoute.builder()
      .origin(origin, bearing, BEARING_TOLERANCE)
      .routeOptions(options);

    List<Point> remainingWaypoints = RouteUtils.calculateRemainingWaypoints(progress);
    if (remainingWaypoints == null) {
      Timber.e("An error occurred fetching a new route");
      return null;
    }
    addRouteProfile(routeProfile, builder);
    addDestination(remainingWaypoints, builder);
    addWaypoints(remainingWaypoints, builder);
    return builder;
  }

  private void addLocaleAndUnitType(NavigationRoute.Builder builder) {
    String voiceUnitType = NavigationUnitType.getDirectionsCriteriaUnitType(unitType, locale);
    builder.language(locale).voiceUnits(voiceUnitType);
  }

  private static void addRouteProfile(String routeProfile, NavigationRoute.Builder builder) {
    if (!TextUtils.isEmpty(routeProfile)) {
      builder.profile(routeProfile);
    }
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

  private boolean isValidProgress(Location location, RouteProgress routeProgress) {
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
    for (RouteEngineListener listener : routeEngineListeners) {
      listener.onResponseReceived(response, routeProgress);
    }
  }

  private void updateListenersWithError(Throwable throwable) {
    for (RouteEngineListener listener : routeEngineListeners) {
      listener.onErrorReceived(throwable);
    }
  }
}

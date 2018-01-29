package com.mapbox.services.android.navigation.ui.v5.route;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RouteViewModel extends AndroidViewModel implements Callback<DirectionsResponse> {

  public final MutableLiveData<DirectionsRoute> route = new MutableLiveData<>();
  public final MutableLiveData<Point> destination = new MutableLiveData<>();
  public final MutableLiveData<String> requestErrorMessage = new MutableLiveData<>();
  private Point origin;
  private Location rawLocation;
  private RouteOptions routeOptions;
  private String routeProfile;
  private String unitType;
  private Locale language;

  public RouteViewModel(@NonNull Application application) {
    super(application);
  }

  /**
   * A new directions response has been received.
   * <p>
   * The {@link DirectionsResponse} is validated.
   * If navigation is running, {@link MapboxNavigation} is updated and reroute state is dismissed.
   * If not, navigation is started.
   *
   * @param call     used to request the new {@link DirectionsRoute}
   * @param response contains the new {@link DirectionsRoute}
   * @since 0.6.0
   */
  @Override
  public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
    if (validRouteResponse(response)) {
      route.setValue(response.body().routes().get(0));
    }
  }

  @Override
  public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
    requestErrorMessage.setValue(throwable.getMessage());
  }

  public void updateRawLocation(@NonNull Location rawLocation) {
    this.rawLocation = rawLocation;
  }

  /**
   * Checks the options used to launch this {@link com.mapbox.services.android.navigation.ui.v5.NavigationView}.
   * <p>
   * Will launch with either a {@link DirectionsRoute} or pair of {@link Point}s.
   *
   * @param options holds either a set of {@link Point} coordinates or a {@link DirectionsRoute}
   */
  public void extractRouteOptions(NavigationViewOptions options) {
    updateUnitType(options.navigationOptions().unitType());
    if (launchWithRoute(options)) {
      extractRouteFromOptions(options);
    } else {
      extractCoordinatesFromOptions(options);
    }
  }

  /**
   * Updates the request unit type based on what was set in
   * {@link NavigationViewOptions}.
   *
   * @param unitType to be used for route requests
   */
  private void updateUnitType(int unitType) {
    boolean isImperialUnitType = unitType == NavigationUnitType.TYPE_IMPERIAL;
    if (isImperialUnitType) {
      this.unitType = DirectionsCriteria.IMPERIAL;
    } else {
      this.unitType = DirectionsCriteria.METRIC;
    }
  }

  /**
   * Requests a new {@link DirectionsRoute}.
   * <p>
   * Will use {@link Location} bearing if we have a rawLocation with bearing.
   * <p>
   * Called when an off-route event is fired and a new {@link DirectionsRoute}
   * is needed to continue navigating.
   *
   * @param offRouteEvent found from off-route event
   */
  public void fetchRouteFromOffRouteEvent(OffRouteEvent offRouteEvent) {
    if (OffRouteEvent.isValid(offRouteEvent)) {
      Double bearing = null;
      if (rawLocation != null) {
        bearing = rawLocation.hasBearing() ? Float.valueOf(rawLocation.getBearing()).doubleValue() : null;
      }

      NavigationRoute.Builder builder = NavigationRoute.builder()
        .accessToken(Mapbox.getAccessToken())
        .origin(offRouteEvent.getNewOrigin(), bearing, 90d);

      // Set build options with cached configuration
      builder.routeOptions(routeOptions);

      // Calculate the remaining waypoints based on the route progress
      List<Point> remainingWaypoints = RouteUtils.calculateRemainingWaypoints(offRouteEvent.getRouteProgress());
      if (remainingWaypoints != null && !remainingWaypoints.isEmpty()) {
        builder.destination(remainingWaypoints.remove(remainingWaypoints.size() - 1));
        addWaypoints(remainingWaypoints, builder);
      }

      // Add any overridden values based on NavigationViewOptions
      addNavigationViewOptions(builder);

      builder.build().getRoute(this);
    }
  }

  private void fetchRouteFromCoordinates(Point origin, Point destination) {
    NavigationRoute.Builder builder = NavigationRoute.builder()
      .accessToken(Mapbox.getAccessToken())
      .origin(origin)
      .destination(destination);
    // Add any overridden values based on NavigationViewOptions
    addNavigationViewOptions(builder);

    builder.build().getRoute(this);
  }

  /**
   * Check if the given {@link NavigationViewOptions} has been launched with a {@link DirectionsRoute}.
   *
   * @param options possibly containing route
   * @return true if route found, false if not
   */
  private boolean launchWithRoute(NavigationViewOptions options) {
    return options.directionsRoute() != null;
  }

  /**
   * Extracts the {@link DirectionsRoute}, adds a destination marker,
   * and starts navigation.
   *
   * @param options containing route
   */
  private void extractRouteFromOptions(NavigationViewOptions options) {
    DirectionsRoute route = options.directionsRoute();
    if (route != null) {
      cacheRouteOptions(route.routeOptions());
      cacheRouteProfile(options);
      cacheRouteLanguage(options);
      this.route.setValue(route);
    }
  }

  /**
   * Extracts the {@link Point} coordinates, adds a destination marker,
   * and fetches a route with the coordinates.
   *
   * @param options containing origin and destination
   */
  private void extractCoordinatesFromOptions(NavigationViewOptions options) {
    if (options.origin() != null && options.destination() != null) {
      cacheRouteProfile(options);
      cacheRouteLanguage(options);
      origin = options.origin();
      destination.setValue(options.destination());
      fetchRouteFromCoordinates(origin, destination.getValue());
    }
  }

  private void cacheRouteOptions(RouteOptions routeOptions) {
    this.routeOptions = routeOptions;
    cacheRouteDestination();
  }

  /**
   * Looks at the given {@link DirectionsRoute} and extracts the destination based on
   * the last {@link LegStep} maneuver.
   */
  private void cacheRouteDestination() {
    if (routeOptions != null && !routeOptions.coordinates().isEmpty()) {
      List<Point> coordinates = routeOptions.coordinates();
      Point destinationPoint = coordinates.get(coordinates.size() - 1);
      destination.setValue(destinationPoint);
    }
  }

  /**
   * Looks for a route profile provided by {@link NavigationViewOptions} to be
   * stored for reroute requests.
   *
   * @param options to look for set profile
   */
  private void cacheRouteProfile(NavigationViewOptions options) {
    routeProfile = options.directionsProfile();
  }

  /**
   * Looks for a route language provided by {@link NavigationViewOptions} to be
   * stored for reroute requests.
   *
   * @param options to look for set language
   */
  private void cacheRouteLanguage(NavigationViewOptions options) {
    language = options.directionsLanguage();
  }

  /**
   * Checks if we have at least one {@link DirectionsRoute} in the given
   * {@link DirectionsResponse}.
   *
   * @param response to be checked
   * @return true if valid, false if not
   */
  private boolean validRouteResponse(Response<DirectionsResponse> response) {
    return response.body() != null
      && !response.body().routes().isEmpty();
  }

  private void addWaypoints(List<Point> remainingCoordinates, NavigationRoute.Builder builder) {
    if (!remainingCoordinates.isEmpty()) {
      for (Point coordinate : remainingCoordinates) {
        builder.addWaypoint(coordinate);
      }
    }
  }

  private void addNavigationViewOptions(NavigationRoute.Builder builder) {
    if (routeProfile != null) {
      builder.profile(routeProfile);
    }
    if (language != null) {
      builder.language(language);
    }
  }
}

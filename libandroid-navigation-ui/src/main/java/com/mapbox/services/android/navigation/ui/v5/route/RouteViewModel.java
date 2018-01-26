package com.mapbox.services.android.navigation.ui.v5.route;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.location.Location;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationUnitType;

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
   * Called when an off-route event is fired and a new {@link DirectionsRoute}
   * is needed to continue navigating.
   *
   * @param newOrigin found from off-route event
   */
  public void fetchRouteNewOrigin(Point newOrigin) {
    if (newOrigin != null && destination.getValue() != null) {
      fetchRoute(newOrigin, destination.getValue());
    }
  }

  /**
   * Requests a new {@link DirectionsRoute}.
   * <p>
   * Will use {@link Location} bearing if we have a rawLocation with bearing.
   *
   * @param origin      start point
   * @param destination end point
   */
  private void fetchRoute(Point origin, Point destination) {
    if (origin != null && destination != null) {

      Double bearing = null;
      if (rawLocation != null) {
        bearing = rawLocation.hasBearing() ? Float.valueOf(rawLocation.getBearing()).doubleValue() : null;
      }

      NavigationRoute.builder()
        .accessToken(Mapbox.getAccessToken())
        .origin(origin, bearing, 90d)
        .voiceUnits(unitType)
        .profile(routeProfile)
        .language(language)
        .destination(destination).build().getRoute(this);
    }
  }

  private void fetchRouteFromCoordinates() {
    fetchRoute(origin, destination.getValue());
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
      cacheRouteProfile(options, route);
      cacheRouteLanguage(options, route);
      cacheRouteDestination(route);
      this.route.setValue(route);
    }
  }

  /**
   * Looks at the given {@link DirectionsRoute} and extracts the destination based on
   * the last {@link LegStep} maneuver.
   *
   * @param route to extract destination from
   */
  private void cacheRouteDestination(DirectionsRoute route) {
    RouteLeg lastLeg = route.legs().get(route.legs().size() - 1);
    LegStep lastStep = lastLeg.steps().get(lastLeg.steps().size() - 1);
    destination.setValue(lastStep.maneuver().location());
  }

  /**
   * Looks for a route profile provided by {@link NavigationViewOptions} to be
   * stored for reroute requests.
   * <p>
   * If not found, look at the {@link com.mapbox.api.directions.v5.models.RouteOptions} for
   * the profile from the original route.
   *
   * @param options to look for set profile
   * @param route   as backup if view options profile not found
   */
  private void cacheRouteProfile(NavigationViewOptions options, DirectionsRoute route) {
    String profile = options.directionsProfile();
    routeProfile = profile != null ? profile : route.routeOptions().profile();
  }

  /**
   * Looks for a route language provided by {@link NavigationViewOptions} to be
   * stored for reroute requests.
   * <p>
   * If not found, look at the {@link com.mapbox.api.directions.v5.models.RouteOptions} for
   * the language from the original route.
   *
   * @param options to look for set language
   * @param route   as backup if view options language not found
   */
  private void cacheRouteLanguage(NavigationViewOptions options, DirectionsRoute route) {
    if (options.directionsLanguage() != null) {
      language = options.directionsLanguage();
    } else if (!TextUtils.isEmpty(route.routeOptions().language())) {
      language = new Locale(route.routeOptions().language());
    } else {
      language = Locale.getDefault();
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
      String profile = options.directionsProfile();
      routeProfile = profile != null ? profile : DirectionsCriteria.PROFILE_DRIVING_TRAFFIC;
      origin = options.origin();
      destination.setValue(options.destination());
      fetchRouteFromCoordinates();
    }
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
}

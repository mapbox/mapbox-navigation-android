package com.mapbox.services.android.navigation.ui.v5.route;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

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
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.services.android.navigation.v5.route.RouteEngine.buildRouteRequestFromCurrentLocation;

public class RouteViewModel extends AndroidViewModel implements Callback<DirectionsResponse> {

  public final MutableLiveData<DirectionsRoute> route = new MutableLiveData<>();
  public final MutableLiveData<Point> destination = new MutableLiveData<>();
  public final MutableLiveData<String> requestErrorMessage = new MutableLiveData<>();
  private Location rawLocation;
  private RouteOptions routeOptions;
  private String routeProfile;
  private String unitType;
  private Locale locale;

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
  public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
    if (validRouteResponse(response)) {
      route.setValue(response.body().routes().get(0));
    }
  }

  @Override
  public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
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
    extractLocale(options);
    extractUnitType(options);

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
   * @param options possibly containing unitType
   */
  private void extractUnitType(NavigationViewOptions options) {
    unitType = NavigationUnitType.getDirectionsCriteriaUnitType(options.navigationOptions().unitType());
  }

  /**
   * Requests a new {@link DirectionsRoute}.
   * <p>
   * Will use {@link Location} bearing if we have a rawLocation with bearing.
   * <p>
   * Called when an off-route event is fired and a new {@link DirectionsRoute}
   * is needed to continue navigating.
   *
   * @param event found from off-route event
   */
  public void fetchRouteFromOffRouteEvent(OffRouteEvent event) {
    if (OffRouteEvent.isValid(event)) {
      Double bearing = null;
      if (rawLocation != null) {
        bearing = rawLocation.hasBearing() ? Float.valueOf(rawLocation.getBearing()).doubleValue() : null;
      }
      Point origin = event.getNewOrigin();
      RouteProgress progress = event.getRouteProgress();
      NavigationRoute.Builder builder = buildRouteRequestFromCurrentLocation(origin, bearing, progress);
      if (builder != null) {
        addNavigationViewOptions(builder);
        builder.build().getRoute(this);
      }
    }
  }

  private void fetchRouteFromCoordinates(Point origin, Point destination) {
    NavigationRoute.Builder builder = NavigationRoute.builder()
      .accessToken(Mapbox.getAccessToken())
      .origin(origin)
      .destination(destination);
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
      cacheRouteInformation(options, route);
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
      cacheRouteLanguage(options, null);
      Point origin = options.origin();
      destination.setValue(options.destination());
      fetchRouteFromCoordinates(origin, destination.getValue());
    }
  }

  /**
   * Checks if we have at least one {@link DirectionsRoute} in the given
   * {@link DirectionsResponse}.
   *
   * @param response to be checked
   * @return true if valid, false if not
   */
  private static boolean validRouteResponse(Response<DirectionsResponse> response) {
    return response.body() != null
      && !response.body().routes().isEmpty();
  }

  private void addNavigationViewOptions(NavigationRoute.Builder builder) {
    if (routeProfile != null) {
      builder.profile(routeProfile);
    }
    builder
      .language(locale)
      .voiceUnits(unitType);
  }

  private void cacheRouteInformation(NavigationViewOptions options, DirectionsRoute route) {
    cacheRouteOptions(route.routeOptions());
    cacheRouteProfile(options);
    cacheRouteLanguage(options, route);
  }

  private void cacheRouteOptions(RouteOptions routeOptions) {
    this.routeOptions = routeOptions;
    cacheRouteDestination();
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
   * Looks at the given {@link DirectionsRoute} and extracts the destination based on
   * the last {@link LegStep} maneuver.
   */
  private void cacheRouteDestination() {
    if (routeOptions != null && !routeOptions.coordinates().isEmpty()) {
      List<Point> coordinates = routeOptions.coordinates();
      int destinationCoordinate = coordinates.size() - 1;
      Point destinationPoint = coordinates.get(destinationCoordinate);
      destination.setValue(destinationPoint);
    }
  }

  /**
   * Looks for a route locale provided by {@link NavigationViewOptions} to be
   * stored for reroute requests.
   *
   * @param options to look for set locale
   */
  private void extractLocale(NavigationViewOptions options) {
    locale = LocaleUtils.getNonNullLocale(this.getApplication(), options.navigationOptions().locale());
  }

  private void cacheRouteLanguage(NavigationViewOptions options, @Nullable DirectionsRoute route) {
    if (options.navigationOptions().locale() != null) {
      locale = options.navigationOptions().locale();
    } else if (route != null && !TextUtils.isEmpty(route.routeOptions().language())) {
      locale = new Locale(route.routeOptions().language());
    } else {
      locale = Locale.getDefault();
    }
  }
}

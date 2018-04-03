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
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
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

  private static final int FIRST_ROUTE = 0;
  private static final int ONE_ROUTE = 1;
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
    processRoute(response);
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
        builder.alternatives(true);
        builder.build().getRoute(this);
      }
    }
  }

  private void processRoute(@NonNull Response<DirectionsResponse> response) {
    if (isValidRoute(response)) {
      List<DirectionsRoute> routes = response.body().routes();
      DirectionsRoute bestRoute = routes.get(FIRST_ROUTE);
      DirectionsRoute chosenRoute = route.getValue();
      if (isNavigationRunning(chosenRoute)) {
        bestRoute = obtainMostSimilarRoute(routes, bestRoute, chosenRoute);
      }
      route.setValue(bestRoute);
    }
  }

  /**
   * Checks if we have at least one {@link DirectionsRoute} in the given
   * {@link DirectionsResponse}.
   *
   * @param response to be checked
   * @return true if valid, false if not
   */
  private boolean isValidRoute(Response<DirectionsResponse> response) {
    return response.body() != null && !response.body().routes().isEmpty();
  }

  private boolean isNavigationRunning(DirectionsRoute chosenRoute) {
    return chosenRoute != null;
  }

  private DirectionsRoute obtainMostSimilarRoute(List<DirectionsRoute> routes, DirectionsRoute currentBestRoute,
                                                 DirectionsRoute chosenRoute) {
    DirectionsRoute mostSimilarRoute = currentBestRoute;
    if (routes.size() > ONE_ROUTE) {
      mostSimilarRoute = findMostSimilarRoute(chosenRoute, routes);
    }
    return mostSimilarRoute;
  }

  private DirectionsRoute findMostSimilarRoute(DirectionsRoute chosenRoute, List<DirectionsRoute> routes) {
    int routeIndex = 0;
    String chosenRouteLegDescription = obtainRouteLegDescriptionFrom(chosenRoute);
    int minSimilarity = Integer.MAX_VALUE;
    for (int index = 0; index < routes.size(); index++) {
      String routeLegDescription = obtainRouteLegDescriptionFrom(routes.get(index));
      int currentSimilarity = DamerauLevenshteinAlgorithm.execute(chosenRouteLegDescription, routeLegDescription);
      if (currentSimilarity < minSimilarity) {
        minSimilarity = currentSimilarity;
        routeIndex = index;
      }
    }
    return routes.get(routeIndex);
  }

  private String obtainRouteLegDescriptionFrom(DirectionsRoute route) {
    List<RouteLeg> routeLegs = route.legs();
    StringBuilder routeLegDescription = new StringBuilder();
    for (RouteLeg leg : routeLegs) {
      routeLegDescription.append(leg.summary());
    }
    return routeLegDescription.toString();
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

  /**
   * Updates the request unit type based on what was set in
   * {@link NavigationViewOptions}. Must be called after extractLocale.
   *
   *
   * @param options possibly containing unitType
   */
  private void extractUnitType(NavigationViewOptions options) {
    MapboxNavigationOptions navigationOptions = options.navigationOptions();
    unitType = NavigationUnitType.getDirectionsCriteriaUnitType(navigationOptions.unitType(), locale);
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
   * Looks for a route profile provided by {@link NavigationViewOptions} to be
   * stored for reroute requests.
   *
   * @param options to look for set profile
   */
  private void cacheRouteProfile(NavigationViewOptions options) {
    routeProfile = options.directionsProfile();
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

  private void fetchRouteFromCoordinates(Point origin, Point destination) {
    NavigationRoute.Builder builder = NavigationRoute.builder()
      .accessToken(Mapbox.getAccessToken())
      .origin(origin)
      .destination(destination);
    addNavigationViewOptions(builder);
    builder.build().getRoute(this);
  }

  private void addNavigationViewOptions(NavigationRoute.Builder builder) {
    if (!TextUtils.isEmpty(routeProfile)) {
      builder.profile(routeProfile);
    }
    builder
      .language(locale)
      .voiceUnits(unitType);
  }
}

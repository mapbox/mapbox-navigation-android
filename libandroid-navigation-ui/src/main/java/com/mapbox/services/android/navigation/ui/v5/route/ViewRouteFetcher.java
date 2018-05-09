package com.mapbox.services.android.navigation.ui.v5.route;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.route.RouteFetcher;
import com.mapbox.services.android.navigation.v5.route.RouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

import java.util.List;
import java.util.Locale;

public class ViewRouteFetcher extends RouteFetcher implements RouteListener {

  private static final int FIRST_ROUTE = 0;
  private static final int ONE_ROUTE = 1;

  private final ViewRouteListener listener;
  private RouteOptions routeOptions;
  private DirectionsRoute currentRoute;
  private Location rawLocation;

  public ViewRouteFetcher(ViewRouteListener listener) {
    this.listener = listener;
    addRouteListener(this);
  }

  @Override
  public void onResponseReceived(DirectionsResponse response, @Nullable RouteProgress routeProgress) {
    processRoute(response);
  }

  @Override
  public void onErrorReceived(Throwable throwable) {
    listener.onRouteRequestError(throwable);
  }

  /**
   * Checks the options used to launch this {@link com.mapbox.services.android.navigation.ui.v5.NavigationView}.
   * <p>
   * Will launch with either a {@link DirectionsRoute} or pair of {@link Point}s.
   *
   * @param options holds either a set of {@link Point} coordinates or a {@link DirectionsRoute}
   */
  public void extractRouteOptions(Context context, NavigationViewOptions options) {
    extractLocale(context, options);
    extractUnitType(options);

    if (launchWithRoute(options)) {
      extractRouteFromOptions(options);
    } else {
      extractCoordinatesFromOptions(options);
    }
  }

  public void fetchRouteFromOffRouteEvent(OffRouteEvent event) {
    if (OffRouteEvent.isValid(event)) {
      RouteProgress routeProgress = event.getRouteProgress();
      findRouteFromRouteProgress(rawLocation, routeProgress);
    }
  }

  public void updateRawLocation(@NonNull Location rawLocation) {
    this.rawLocation = rawLocation;
  }

  private void extractLocale(Context context, NavigationViewOptions options) {
    updateLanguage(LocaleUtils.getNonNullLocale(context, options.navigationOptions().locale()));
  }

  private void extractUnitType(NavigationViewOptions options) {
    MapboxNavigationOptions navigationOptions = options.navigationOptions();
    updateUnitType(navigationOptions.unitType());
  }

  private boolean launchWithRoute(NavigationViewOptions options) {
    return options.directionsRoute() != null;
  }

  private void extractRouteFromOptions(NavigationViewOptions options) {
    DirectionsRoute route = options.directionsRoute();
    if (route != null) {
      cacheRouteInformation(options, route);
      updateCurrentRoute(route);
    }
  }

  private void cacheRouteInformation(NavigationViewOptions options, DirectionsRoute route) {
    cacheRouteOptions(route.routeOptions());
    cacheRouteProfile(options);
    cacheRouteLanguage(options, route);
  }

  private void cacheRouteProfile(NavigationViewOptions options) {
    String routeProfile = options.directionsProfile();
    updateRouteProfile(routeProfile);
  }

  private void cacheRouteLanguage(NavigationViewOptions options, @Nullable DirectionsRoute route) {
    if (options.navigationOptions().locale() != null) {
      updateLanguage(options.navigationOptions().locale());
    } else if (route != null && !TextUtils.isEmpty(route.routeOptions().language())) {
      updateLanguage(new Locale(route.routeOptions().language()));
    } else {
      updateLanguage(Locale.getDefault());
    }
  }

  private void cacheRouteOptions(RouteOptions routeOptions) {
    this.routeOptions = routeOptions;
    cacheRouteDestination();
  }

  private void cacheRouteDestination() {
    boolean hasValidCoordinates = routeOptions != null && !routeOptions.coordinates().isEmpty();
    if (hasValidCoordinates) {
      List<Point> coordinates = routeOptions.coordinates();
      int destinationCoordinate = coordinates.size() - 1;
      Point destinationPoint = coordinates.get(destinationCoordinate);
      listener.onDestinationSet(destinationPoint);
    }
  }

  private void extractCoordinatesFromOptions(NavigationViewOptions options) {
    Point destination = options.destination();
    if (options.origin() != null && options.destination() != null) {
      cacheRouteProfile(options);
      cacheRouteLanguage(options, null);
      Point origin = options.origin();
      listener.onDestinationSet(destination);
      findRouteFromOriginToDestination(origin, destination);
    }
  }

  private void processRoute(@NonNull DirectionsResponse response) {
    if (isValidRoute(response)) {
      List<DirectionsRoute> routes = response.routes();
      DirectionsRoute bestRoute = routes.get(FIRST_ROUTE);
      DirectionsRoute chosenRoute = currentRoute;
      if (isNavigationRunning(chosenRoute)) {
        bestRoute = obtainMostSimilarRoute(routes, bestRoute, chosenRoute);
      }
      updateCurrentRoute(bestRoute);
    }
  }

  private void updateCurrentRoute(DirectionsRoute currentRoute) {
    this.currentRoute = currentRoute;
    listener.onRouteUpdate(currentRoute);
  }

  private boolean isValidRoute(DirectionsResponse response) {
    return response != null && !response.routes().isEmpty();
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
}

package com.mapbox.services.android.navigation.ui.v5.route;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.v5.route.RouteFetcher;
import com.mapbox.services.android.navigation.v5.route.RouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

public class ViewRouteFetcher extends RouteFetcher implements RouteListener {

  private static final int FIRST_ROUTE = 0;
  private static final int ONE_ROUTE = 1;

  private final ViewRouteListener listener;
  private RouteOptions routeOptions;
  private DirectionsRoute currentRoute;
  private Location rawLocation;

  public ViewRouteFetcher(Context context, String accessToken, ViewRouteListener listener) {
    super(context, accessToken);
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
   * Will launch with a {@link DirectionsRoute}.
   *
   * @param options holds a {@link DirectionsRoute}
   */
  public void extractRouteOptions(NavigationViewOptions options) {
    extractRouteFromOptions(options);
  }

  /**
   * Fetches the route from the off-route event
   *
   * @param event from which the route progress is extracted
   */
  public void fetchRouteFromOffRouteEvent(OffRouteEvent event) {
    if (OffRouteEvent.isValid(event)) {
      RouteProgress routeProgress = event.getRouteProgress();
      findRouteFromRouteProgress(rawLocation, routeProgress);
    }
  }

  /**
   * Calculates the remaining coordinates in the given OffRouteEvent
   *
   * @param event from which the progress is extracted
   * @return List of remaining coordinates
   */
  public List<Point> calculateRemainingCoordinates(OffRouteEvent event) {
    if (OffRouteEvent.isValid(event)) {
      RouteProgress routeProgress = event.getRouteProgress();
      return routeUtils.calculateRemainingWaypoints(routeProgress);
    } else {
      return null;
    }
  }

  /**
   * Updates this object's awareness of the raw location
   *
   * @param rawLocation to set
   */
  public void updateRawLocation(@NonNull Location rawLocation) {
    this.rawLocation = rawLocation;
  }

  private void extractRouteFromOptions(NavigationViewOptions options) {
    DirectionsRoute route = options.directionsRoute();
    cacheRouteOptions(route.routeOptions());
    updateCurrentRoute(route);
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

package com.mapbox.navigation.ui;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.navigation.base.route.model.Route;
import com.mapbox.api.directions.v5.models.RouteLeg;

import java.util.List;

class RouteComparator {

  private static final int FIRST_ROUTE = 0;
  private static final int ONE_ROUTE = 1;
  private final NavigationViewRouter navigationViewRouter;

  RouteComparator(NavigationViewRouter navigationViewRouter) {
    this.navigationViewRouter = navigationViewRouter;
  }

  void compare(@NonNull DirectionsResponse response, @Nullable Route chosenRoute) {
    if (isValidRoute(response)) {
      List<Route> routes = response.routes();
      Route bestRoute = routes.get(FIRST_ROUTE);
      if (isNavigationRunning(chosenRoute)) {
        bestRoute = findMostSimilarRoute(routes, bestRoute, chosenRoute);
      }
      navigationViewRouter.updateCurrentRoute(bestRoute);
    }
  }

  private Route findMostSimilarRoute(List<Route> routes, Route currentBestRoute,
                                               Route chosenRoute) {
    Route mostSimilarRoute = currentBestRoute;
    if (routes.size() > ONE_ROUTE) {
      mostSimilarRoute = compareRoutes(chosenRoute, routes);
    }
    return mostSimilarRoute;
  }

  private Route compareRoutes(Route chosenRoute, List<Route> routes) {
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

  private String obtainRouteLegDescriptionFrom(Route route) {
    List<RouteLeg> routeLegs = route.legs();
    StringBuilder routeLegDescription = new StringBuilder();
    for (RouteLeg leg : routeLegs) {
      routeLegDescription.append(leg.summary());
    }
    return routeLegDescription.toString();
  }

  private boolean isValidRoute(DirectionsResponse response) {
    return response != null && !response.routes().isEmpty();
  }

  private boolean isNavigationRunning(Route chosenRoute) {
    return chosenRoute != null;
  }
}
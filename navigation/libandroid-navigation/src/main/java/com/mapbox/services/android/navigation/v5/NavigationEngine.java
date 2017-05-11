package com.mapbox.services.android.navigation.v5;


import android.location.Location;

import com.mapbox.services.android.navigation.v5.listeners.AlertLevelChangeListener;
import com.mapbox.services.android.navigation.v5.listeners.OffRouteListener;
import com.mapbox.services.android.navigation.v5.listeners.ProgressChangeListener;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;

import java.util.List;

class NavigationEngine {

  private DirectionsRoute directionsRoute;
  private RouteProgress previousRouteProgress;
  private int stepIndex;
  private int legIndex;
  private MapboxNavigationOptions options;
  private boolean isSnapEnabled;
  private boolean previousUserOffRoute;
  private List<AlertLevelChangeListener> alertLevelChangeListeners;
  private List<ProgressChangeListener> progressChangeListeners;
  private List<OffRouteListener> offRouteListeners;

  NavigationEngine(MapboxNavigationOptions options, boolean isSnapEnabled) {
    this.isSnapEnabled = isSnapEnabled;
    this.options = options;
    stepIndex = 0;
    legIndex = 0;
  }

  void onLocationChanged(DirectionsRoute directionsRoute, Location location) {
    this.directionsRoute = directionsRoute;
    // if the previousRouteProgress is null, the route has just begun and one needs to be created
    if (previousRouteProgress == null) {
      previousRouteProgress = new RouteProgress(directionsRoute, location, 0, 0, NavigationConstants.NONE_ALERT_LEVEL);
    }

    // Get the new alert level
    AlertLevelState alertLevelState = new AlertLevelState(location, previousRouteProgress, stepIndex, legIndex, options);
    int alertLevel = alertLevelState.getNewAlertLevel();
    stepIndex = alertLevelState.getStepIndex();
    legIndex = alertLevelState.getLegIndex();

    // Create a new RouteProgress object using the latest user location
    RouteProgress routeProgress = new RouteProgress(directionsRoute, location, legIndex, stepIndex, alertLevel);

    // Determine if the user is off route or not
    UserOffRouteState userOffRouteState = new UserOffRouteState(location, routeProgress, options);
    boolean isUserOffRoute = userOffRouteState.isUserOffRoute();

    // Snap location to the route if they aren't off route and return the location object
    if (isSnapEnabled && !isUserOffRoute) {
      SnapLocation snapLocation = new SnapLocation(location, routeProgress, options);
      location = snapLocation.getSnappedLocation();
    }

    notifyAlertLevelChange(routeProgress);
    notifyOffRouteChange(isUserOffRoute, location);
    notifyProgressChange(location, routeProgress);

    previousRouteProgress = routeProgress;
  }

  private void notifyAlertLevelChange(RouteProgress routeProgress) {
    if (routeProgress.getCurrentLegProgress().getStepIndex()
      != previousRouteProgress.getCurrentLegProgress().getStepIndex()
      || previousRouteProgress.getAlertUserLevel() != routeProgress.getAlertUserLevel()) {

      for (AlertLevelChangeListener alertLevelChangeListener : alertLevelChangeListeners) {
        alertLevelChangeListener.onAlertLevelChange(routeProgress.getAlertUserLevel(), routeProgress);
      }
    }
  }

  private void notifyOffRouteChange(boolean isUserOffRoute, Location location) {
    // Only report user off route once.
    if (isUserOffRoute && !previousUserOffRoute) {
      for (OffRouteListener offRouteListener : offRouteListeners) {
        offRouteListener.userOffRoute(location);
      }
    }
    previousUserOffRoute = isUserOffRoute;
  }

  private void notifyProgressChange(Location location, RouteProgress routeProgress) {
    for (ProgressChangeListener progressChangeListener : progressChangeListeners) {
      progressChangeListener.onProgressChange(location, routeProgress);
    }
  }

  DirectionsRoute getDirectionsRoute() {
    return directionsRoute;
  }

  void setDirectionsRoute(DirectionsRoute directionsRoute) {
    this.directionsRoute = directionsRoute;
  }

  public boolean isSnapEnabled() {
    return isSnapEnabled;
  }

  void setSnapEnabled(boolean snapEnabled) {
    isSnapEnabled = snapEnabled;
  }

  public MapboxNavigationOptions getOptions() {
    return options;
  }

  public void setOptions(MapboxNavigationOptions options) {
    this.options = options;
  }

  void setAlertLevelChangeListeners(List<AlertLevelChangeListener> alertLevelChangeListeners) {
    this.alertLevelChangeListeners = alertLevelChangeListeners;
  }

  void setProgressChangeListeners(List<ProgressChangeListener> progressChangeListeners) {
    this.progressChangeListeners = progressChangeListeners;
  }

  void setOffRouteListeners(List<OffRouteListener> offRouteListeners) {
    this.offRouteListeners = offRouteListeners;
  }

}

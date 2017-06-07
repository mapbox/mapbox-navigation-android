package com.mapbox.services.android.navigation.v5;


import android.location.Location;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.mapbox.services.android.navigation.v5.listeners.AlertLevelChangeListener;
import com.mapbox.services.android.navigation.v5.listeners.OffRouteListener;
import com.mapbox.services.android.navigation.v5.listeners.ProgressChangeListener;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * The navigation engine makes use of {@link UserOffRouteState}, {@link AlertLevelState}, etc. to first create a new
 * route progress object and then to invoke the appropriate callbacks depending on the navigation state.
 *
 * @since 0.2.0
 */
class NavigationEngine {

  // Listeners
  private CopyOnWriteArrayList<AlertLevelChangeListener> alertLevelChangeListeners;
  private CopyOnWriteArrayList<ProgressChangeListener> progressChangeListeners;
  private CopyOnWriteArrayList<OffRouteListener> offRouteListeners;

  // Navigation state information
  private RouteProgress previousRouteProgress;
  private MapboxNavigationOptions options;
  private long timeIntervalSinceLastOffRoute;
  private Location previousLocation;
  private boolean isSnapEnabled;
  private int stepIndex;
  private int legIndex;

  /**
   * Constructs a new navigation engine instance.
   *
   * @param options       the initial {@link MapboxNavigationOptions} to be used (this can be updated using the setter)
   * @param isSnapEnabled boolean true if the snapping to route features enabled, otherwise false
   * @since 0.2.0
   */
  NavigationEngine(@NonNull MapboxNavigationOptions options, boolean isSnapEnabled) {
    this.isSnapEnabled = isSnapEnabled;
    this.options = options;
    stepIndex = 0;
    legIndex = 0;
  }

  /**
   * When the {@link NavigationService} recieves a new location update, this methods called which coordinates the
   * updating of events and creating the new {@link RouteProgress} object.
   *
   * @param directionsRoute takes in the directions route which ensures if a reroute occurs we are using it
   * @param location        the user location
   * @since 0.2.0
   */
  void onLocationChanged(DirectionsRoute directionsRoute, Location location) {
    // if the previousRouteProgress is null, the route has just begun and one needs to be created
    if (previousRouteProgress == null) {
      Position currentPosition = Position.fromCoordinates(location.getLongitude(), location.getLatitude());
      previousRouteProgress = new RouteProgress(directionsRoute, currentPosition,
        0, 0, NavigationConstants.NONE_ALERT_LEVEL);
    }

    if (!TextUtils.equals(directionsRoute.getGeometry(), previousRouteProgress.getRoute().getGeometry())) {
      resetRouteProgress();
    }

    // If the locations the same as previous, no need to recalculate things
    if (location.equals(previousLocation)) {
      return;
    }

    previousLocation = location;

    // Get the new alert level
    AlertLevelState alertLevelState
      = new AlertLevelState(location, previousRouteProgress, stepIndex, legIndex, options);
    int alertLevel = alertLevelState.getNewAlertLevel();
    stepIndex = alertLevelState.getStepIndex();
    legIndex = alertLevelState.getLegIndex();

    SnapLocation snapLocation = new SnapLocation(location,
      previousRouteProgress.getCurrentLegProgress().getCurrentStep(), options);

    // Create a new RouteProgress object using the latest user location
    RouteProgress routeProgress = new RouteProgress(directionsRoute, snapLocation.getUsersCurrentSnappedPosition(),
      legIndex, stepIndex, alertLevel);

    // Determine if the user is off route or not
    UserOffRouteState userOffRouteState = new UserOffRouteState(location, routeProgress, options);
    boolean isUserOffRoute = userOffRouteState.isUserOffRoute();

    // Snap location to the route if they aren't off route and return the location object
    if (isSnapEnabled && !isUserOffRoute) {
      location = snapLocation.getSnappedLocation();
      location.setBearing(snapLocation.snapUserBearing(routeProgress));
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

    if (isUserOffRoute) {
      if (location.getTime() > timeIntervalSinceLastOffRoute
        + TimeUnit.SECONDS.toMillis(NavigationConstants.SECONDS_BEFORE_REROUTE)) {
        for (OffRouteListener offRouteListener : offRouteListeners) {
          offRouteListener.userOffRoute(location);
        }
        timeIntervalSinceLastOffRoute = location.getTime();
      }
    } else {
      timeIntervalSinceLastOffRoute = location.getTime();
    }
  }

  private void notifyProgressChange(Location location, RouteProgress routeProgress) {
    for (ProgressChangeListener progressChangeListener : progressChangeListeners) {
      progressChangeListener.onProgressChange(location, routeProgress);
    }
  }

  private void resetRouteProgress() {
    legIndex = 0;
    stepIndex = 0;
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

  void setAlertLevelChangeListeners(CopyOnWriteArrayList<AlertLevelChangeListener> alertLevelChangeListeners) {
    this.alertLevelChangeListeners = alertLevelChangeListeners;
  }

  void setProgressChangeListeners(CopyOnWriteArrayList<ProgressChangeListener> progressChangeListeners) {
    this.progressChangeListeners = progressChangeListeners;
  }

  void setOffRouteListeners(CopyOnWriteArrayList<OffRouteListener> offRouteListeners) {
    this.offRouteListeners = offRouteListeners;
  }
}

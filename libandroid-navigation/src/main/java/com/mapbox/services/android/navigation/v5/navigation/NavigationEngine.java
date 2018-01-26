package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.List;

import timber.log.Timber;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.bearingMatchesManeuverFinalBearing;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.checkMilestones;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.getSnappedLocation;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.increaseIndex;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.isUserOffRoute;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.legDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.routeDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.shouldCheckFasterRoute;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.stepDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.userSnappedToRoutePosition;

/**
 * This class extends handler thread to run most of the navigation calculations on a separate
 * background thread.
 */
class NavigationEngine extends HandlerThread implements Handler.Callback {

  private static final String THREAD_NAME = "NavThread";
  private RouteProgress previousRouteProgress;
  private List<Point> stepPositions;
  private NavigationIndices indices;
  private Handler responseHandler;
  private Handler workerHandler;
  private Callback callback;

  NavigationEngine(Handler responseHandler, Callback callback) {
    super(THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND);
    this.responseHandler = responseHandler;
    this.callback = callback;
    indices = NavigationIndices.create(0, 0);
  }

  void queueTask(int msgIdentifier, NewLocationModel newLocationModel) {
    workerHandler.obtainMessage(msgIdentifier, newLocationModel).sendToTarget();
  }

  void prepareHandler() {
    workerHandler = new Handler(getLooper(), this);
  }

  @Override
  public boolean handleMessage(Message msg) {
    NewLocationModel newLocationModel = (NewLocationModel) msg.obj;
    handleRequest(newLocationModel);
    return true;
  }

  private void handleRequest(final NewLocationModel newLocationModel) {

    MapboxNavigation mapboxNavigation = newLocationModel.mapboxNavigation();
    boolean snapToRouteEnabled = mapboxNavigation.options().snapToRoute();
    boolean fasterRouteDetectionEnabled = mapboxNavigation.options().enableFasterRouteDetection();

    Location rawLocation = newLocationModel.location();
    RingBuffer recentDistances = newLocationModel.recentDistancesFromManeuverInMeters();

    // Generate a new route progress given the raw location update
    final RouteProgress routeProgress = generateNewRouteProgress(mapboxNavigation, rawLocation, recentDistances);

    // Check milestone list to see if any should be triggered
    final List<Milestone> milestones = checkMilestones(
      previousRouteProgress, routeProgress, mapboxNavigation);

    // Check if user has gone off-route
    final boolean userOffRoute = isUserOffRoute(newLocationModel, routeProgress);

    // Create snapped location if enabled, otherwise return raw location
    final Location location;
    if (!userOffRoute && snapToRouteEnabled) {
      location = getSnappedLocation(mapboxNavigation, rawLocation, routeProgress, stepPositions);
    } else {
      location = rawLocation;
    }

    // Check for faster route only if enabled and not off-route
    final boolean checkFasterRoute = fasterRouteDetectionEnabled && !userOffRoute
      && shouldCheckFasterRoute(newLocationModel, routeProgress);

    previousRouteProgress = routeProgress;

    responseHandler.post(new Runnable() {
      @Override
      public void run() {
        callback.onNewRouteProgress(location, routeProgress);
        callback.onMilestoneTrigger(milestones, routeProgress);
        callback.onUserOffRoute(location, userOffRoute);
        callback.onCheckFasterRoute(location, routeProgress, checkFasterRoute);
      }
    });
  }

  private RouteProgress generateNewRouteProgress(MapboxNavigation mapboxNavigation, Location location,
                                                 RingBuffer recentDistances) {

    Timber.d("NAV-DEBUG ***** ***** ***** Generating new RouteProgress ***** ***** *****");

    DirectionsRoute directionsRoute = mapboxNavigation.getRoute();
    MapboxNavigationOptions options = mapboxNavigation.options();

    if (RouteUtils.isNewRoute(previousRouteProgress, directionsRoute)) {

      Timber.d("NAV-DEBUG ** New route, resetting values");

      // Decode the first steps geometry and hold onto the resulting Position objects till the users
      // on the next step. Indices are both 0 since the user just started on the new route.
      stepPositions = PolylineUtils.decode(
        directionsRoute.legs().get(0).steps().get(0).geometry(), PRECISION_6);

      previousRouteProgress = RouteProgress.builder()
        .stepDistanceRemaining(directionsRoute.legs().get(0).steps().get(0).distance())
        .legDistanceRemaining(directionsRoute.legs().get(0).distance())
        .distanceRemaining(directionsRoute.distance())
        .directionsRoute(directionsRoute)
        .stepIndex(0)
        .legIndex(0)
        .build();

      indices = NavigationIndices.create(0, 0);
    }
    Timber.d("NAV-DEBUG ** legIndex %s, stepIndex %s", indices.legIndex(), indices.stepIndex());

    Point snappedPosition = userSnappedToRoutePosition(location, stepPositions);

    double stepDistanceRemaining = stepDistanceRemaining(
      snappedPosition, indices.legIndex(), indices.stepIndex(), directionsRoute, stepPositions);
    double legDistanceRemaining = legDistanceRemaining(
      stepDistanceRemaining, indices.legIndex(), indices.stepIndex(), directionsRoute);
    double routeDistanceRemaining = routeDistanceRemaining(
      legDistanceRemaining, indices.legIndex(), directionsRoute);

    Timber.d("NAV-DEBUG ** stepDistanceRemaining pre- heading check %s", stepDistanceRemaining);

    boolean bearingMatch = bearingMatchesManeuverFinalBearing(location, previousRouteProgress, options.maxTurnCompletionOffset());
    if (!bearingMatch && stepDistanceRemaining == 0) {
      Timber.d("NAV-DEBUG ** ALERT No bearing match and step distance remaining 0");
    }

    if (bearingMatch && (stepDistanceRemaining < options.maneuverZoneRadius())) {

      if (stepDistanceRemaining != 0) {
        Timber.d("NAV-DEBUG ** ALERT Advancing step with stepDistanceRemaining: %s", stepDistanceRemaining);
      }

      Timber.d("NAV-DEBUG ** Bearing matches final maneuver heading + stepDistanceRemaining < maneuverZone");
      // First increase the indices and then update the majority of information for the new
      // routeProgress.
      indices = increaseIndex(previousRouteProgress, indices);
      Timber.d("NAV-DEBUG ** legIndex %s, stepIndex %s", indices.legIndex(), indices.stepIndex());
      // First increase the indices and then update the majority of information for the new
      stepPositions = PolylineUtils.decode(
        directionsRoute.legs().get(
          indices.legIndex()).steps().get(indices.stepIndex()).geometry(), PRECISION_6);
      snappedPosition = userSnappedToRoutePosition(location, stepPositions);
      stepDistanceRemaining = stepDistanceRemaining(
        snappedPosition, indices.legIndex(), indices.stepIndex(), directionsRoute, stepPositions);
      legDistanceRemaining = legDistanceRemaining(
        stepDistanceRemaining, indices.legIndex(), indices.stepIndex(), directionsRoute);
      routeDistanceRemaining = routeDistanceRemaining(
        legDistanceRemaining, indices.legIndex(), directionsRoute);

      // Remove all distance values from recentDistancesFromManeuverInMeters
      recentDistances.clear();
    }

    Timber.d("NAV-DEBUG ** stepDistanceRemaining %s", stepDistanceRemaining);

    // Create a RouteProgress.create object using the latest user location
    return RouteProgress.builder()
      .stepDistanceRemaining(stepDistanceRemaining)
      .legDistanceRemaining(legDistanceRemaining)
      .distanceRemaining(routeDistanceRemaining)
      .directionsRoute(directionsRoute)
      .stepIndex(indices.stepIndex())
      .legIndex(indices.legIndex())
      .build();
  }

  /**
   * Callbacks for posting back to the Navigation Service once the thread finishes calculations.
   * No matter what, with each new message added to the queue, these callbacks get invoked once
   * finished and within Navigation Service it is determined if the public corresponding listeners
   * need invoking or not; the Navigation event dispatcher class handles those callbacks.
   */
  interface Callback {
    void onNewRouteProgress(Location location, RouteProgress routeProgress);

    void onMilestoneTrigger(List<Milestone> triggeredMilestones, RouteProgress routeProgress);

    void onUserOffRoute(Location location, boolean userOffRoute);

    void onCheckFasterRoute(Location location, RouteProgress routeProgress, boolean checkFasterRoute);
  }
}

package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteCallback;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.List;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.checkBearingForStepCompletion;
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
class NavigationEngine extends HandlerThread implements Handler.Callback, OffRouteCallback {

  private static final String THREAD_NAME = "NavThread";
  private RouteProgress previousRouteProgress;
  private List<Point> stepPositions;
  private NavigationIndices indices;
  private Handler responseHandler;
  private Handler workerHandler;
  private Callback callback;

  private boolean shouldIncreaseStepIndex;
  private boolean shouldClearRecentDistances;

  NavigationEngine(Handler responseHandler, Callback callback) {
    super(THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND);
    this.responseHandler = responseHandler;
    this.callback = callback;
    indices = NavigationIndices.create(0, 0);
  }

  @Override
  public boolean handleMessage(Message msg) {
    NewLocationModel newLocationModel = (NewLocationModel) msg.obj;
    handleRequest(newLocationModel);
    return true;
  }

  @Override
  public void onShouldIncreaseIndex() {
    shouldIncreaseStepIndex = true;
  }

  void queueTask(int msgIdentifier, NewLocationModel newLocationModel) {
    workerHandler.obtainMessage(msgIdentifier, newLocationModel).sendToTarget();
  }

  void prepareHandler() {
    workerHandler = new Handler(getLooper(), this);
  }

  private void handleRequest(final NewLocationModel newLocationModel) {

    final MapboxNavigation mapboxNavigation = newLocationModel.mapboxNavigation();
    boolean snapToRouteEnabled = mapboxNavigation.options().snapToRoute();

    final Location rawLocation = newLocationModel.location();

    // Generate a new route progress given the raw location update
    RouteProgress routeProgress = generateNewRouteProgress(mapboxNavigation, rawLocation);

    // Check if user has gone off-route
    final boolean userOffRoute = isUserOffRoute(newLocationModel, routeProgress, this);

    // If needed, increase step index and generate new route progress
    checkIncreaseStepIndex(mapboxNavigation.getRoute());

    // Check if recent distances from the maneuver should be cleared
    checkRecentDistances(newLocationModel);

    // Check milestone list to see if any should be triggered
    final List<Milestone> milestones = checkMilestones(
      previousRouteProgress, routeProgress, mapboxNavigation);

    // Create snapped location if enabled, otherwise return raw location
    final Location location;
    if (!userOffRoute && snapToRouteEnabled) {
      location = getSnappedLocation(mapboxNavigation, rawLocation, routeProgress, stepPositions);
    } else {
      location = rawLocation;
    }

    // Check for faster route only if enabled and not off-route
    boolean fasterRouteEnabled = mapboxNavigation.options().enableFasterRouteDetection();
    final boolean checkFasterRoute = fasterRouteEnabled && !userOffRoute
      && shouldCheckFasterRoute(newLocationModel, routeProgress);

    // Copy route progress to final object for callback
    final RouteProgress finalRouteProgress = routeProgress;
    previousRouteProgress = finalRouteProgress;

    responseHandler.post(new Runnable() {
      @Override
      public void run() {
        callback.onNewRouteProgress(location, finalRouteProgress);
        callback.onMilestoneTrigger(milestones, finalRouteProgress);
        callback.onUserOffRoute(location, userOffRoute);
        callback.onCheckFasterRoute(location, finalRouteProgress, checkFasterRoute);
      }
    });
  }

  private void checkIncreaseStepIndex(DirectionsRoute route) {
    if (shouldIncreaseStepIndex) {
      advanceStepIndex(route);
      shouldIncreaseStepIndex = false;
    }
  }

  private void checkRecentDistances(NewLocationModel newLocationModel) {
    RingBuffer recentDistances = newLocationModel.distancesAwayFromManeuver();
    if (shouldClearRecentDistances && !recentDistances.isEmpty()) {
      recentDistances.clear();
      shouldClearRecentDistances = false;
    }
  }

  private RouteProgress generateNewRouteProgress(MapboxNavigation mapboxNavigation, Location location) {

    DirectionsRoute directionsRoute = mapboxNavigation.getRoute();
    MapboxNavigationOptions options = mapboxNavigation.options();
    double completionOffset = options.maxTurnCompletionOffset();
    double maneuverZoneRadius = options.maneuverZoneRadius();

    // Check if a new route has been provided
    checkNewRoute(directionsRoute);

    double stepDistanceRemaining = calculateStepDistanceRemaining(location, directionsRoute);
    boolean withinManeuverRadius = stepDistanceRemaining < maneuverZoneRadius;
    boolean bearingMatchesManeuver = checkBearingForStepCompletion(
      location, previousRouteProgress, stepDistanceRemaining, completionOffset
    );

    if (bearingMatchesManeuver && withinManeuverRadius) {
      // Advance the step index and create new step distance remaining
      advanceStepIndex(directionsRoute);
      // Re-calculate the step distance remaining based on the new index
      stepDistanceRemaining = calculateStepDistanceRemaining(location, directionsRoute);
      // Clear any recent distances from the maneuver (maneuver has now changed)
      shouldClearRecentDistances = true;
    }

    int legIndex = indices.legIndex();
    int stepIndex = indices.stepIndex();
    double legDistanceRemaining = legDistanceRemaining(stepDistanceRemaining, legIndex, stepIndex, directionsRoute);
    double routeDistanceRemaining = routeDistanceRemaining(legDistanceRemaining, legIndex, directionsRoute);

    // Create a RouteProgress.create object using the latest user location
    return RouteProgress.builder()
      .stepDistanceRemaining(stepDistanceRemaining)
      .legDistanceRemaining(legDistanceRemaining)
      .distanceRemaining(routeDistanceRemaining)
      .directionsRoute(directionsRoute)
      .stepIndex(stepIndex)
      .legIndex(legIndex)
      .build();
  }

  private void advanceStepIndex(DirectionsRoute directionsRoute) {
    // First increase the indices and then update the majority of information for the new
    // routeProgress.
    indices = increaseIndex(previousRouteProgress, indices);

    // First increase the indices and then update the majority of information for the new
    stepPositions = decodeStepPositions(directionsRoute, indices.legIndex(), indices.stepIndex());
  }

  private List<Point> decodeStepPositions(DirectionsRoute directionsRoute, int legIndex, int stepIndex) {
    // Check for valid legs
    List<RouteLeg> legs = directionsRoute.legs();
    if (legs == null || legs.isEmpty()) {
      return stepPositions;
    }
    // Check for valid steps
    List<LegStep> steps = legs.get(legIndex).steps();
    if (steps == null || steps.isEmpty()) {
      return stepPositions;
    }

    String stepGeometry = steps.get(stepIndex).geometry();
    if (stepGeometry != null) {
      return PolylineUtils.decode(stepGeometry, PRECISION_6);
    }
    return stepPositions;
  }

  private void checkNewRoute(DirectionsRoute directionsRoute) {
    if (RouteUtils.isNewRoute(previousRouteProgress, directionsRoute)) {
      // Decode the first steps geometry and hold onto the resulting Position objects till the users
      // on the next step. Indices are both 0 since the user just started on the new route.
      stepPositions = decodeStepPositions(directionsRoute, 0, 0);

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
  }

  private double calculateStepDistanceRemaining(Location location, DirectionsRoute directionsRoute) {
    Point snappedPosition = userSnappedToRoutePosition(location, stepPositions);
    return stepDistanceRemaining(
      snappedPosition, indices.legIndex(), indices.stepIndex(), directionsRoute, stepPositions
    );
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

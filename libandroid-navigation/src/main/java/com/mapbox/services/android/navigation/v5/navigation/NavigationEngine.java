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
  private List<Point> stepPoints;
  private NavigationIndices indices;
  private Handler responseHandler;
  private Handler workerHandler;
  private Callback callback;

  private boolean shouldIncreaseStepIndex;

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

  /**
   * Takes a new location model and runs all related engine checks against it
   * (off-route, milestones, snapped location, and faster-route).
   * <p>
   * After running through the engines, all data is submitted to {@link NavigationService} via
   * {@link NavigationEngine.Callback}.
   *
   * @param newLocationModel hold location, navigation (with options), and distances away from maneuver
   */
  private void handleRequest(final NewLocationModel newLocationModel) {

    final MapboxNavigation mapboxNavigation = newLocationModel.mapboxNavigation();
    boolean snapToRouteEnabled = mapboxNavigation.options().snapToRoute();
    RingBuffer recentDistances = newLocationModel.distancesAwayFromManeuver();

    final Location rawLocation = newLocationModel.location();

    // Generate a new route progress given the raw location update
    RouteProgress routeProgress = buildNewRouteProgress(mapboxNavigation, rawLocation, recentDistances);

    // Check if user has gone off-route
    final boolean userOffRoute = isUserOffRoute(newLocationModel, routeProgress, this);

    // If needed, increase step index and generate new route progress
    checkIncreaseStepIndex(mapboxNavigation.getRoute(), recentDistances);

    // Check milestone list to see if any should be triggered
    final List<Milestone> milestones = checkMilestones(
      previousRouteProgress, routeProgress, mapboxNavigation);

    // Create snapped location if enabled, otherwise return raw location
    final Location location = buildSnappedLocation(mapboxNavigation, snapToRouteEnabled,
      rawLocation, routeProgress, userOffRoute);

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

  /**
   * Will take a given location update and create a new {@link RouteProgress}
   * based on our calculations of the distances remaining.
   * <p>
   * Also in charge of detecting if a step / leg has finished and incrementing the
   * indices if needed ({@link NavigationEngine#advanceStepIndex(DirectionsRoute, RingBuffer)} handles
   * the decoding of the next step point list).
   *
   * @param mapboxNavigation for the current route / options
   * @param location         for step / leg / route distance remaining
   * @param recentDistances  for advancing the step index
   * @return new route progress along the route
   */
  private RouteProgress buildNewRouteProgress(MapboxNavigation mapboxNavigation, Location location,
                                              RingBuffer recentDistances) {
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
      advanceStepIndex(directionsRoute, recentDistances);
      // Re-calculate the step distance remaining based on the new index
      stepDistanceRemaining = calculateStepDistanceRemaining(location, directionsRoute);
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

  /**
   * If the {@link OffRouteCallback#onShouldIncreaseIndex()} has been called by the
   * {@link com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector}, shouldIncreaseStepIndex
   * will be true and the {@link NavigationIndices} step index needs to be increased by one.
   *
   * @param route to get the next {@link LegStep#geometry()}
   */
  private void checkIncreaseStepIndex(DirectionsRoute route, RingBuffer recentDistances) {
    if (shouldIncreaseStepIndex) {
      advanceStepIndex(route, recentDistances);
      shouldIncreaseStepIndex = false;
    }
  }

  private Location buildSnappedLocation(MapboxNavigation mapboxNavigation, boolean snapToRouteEnabled,
                                        Location rawLocation, RouteProgress routeProgress, boolean userOffRoute) {
    final Location location;
    if (!userOffRoute && snapToRouteEnabled) {
      location = getSnappedLocation(mapboxNavigation, rawLocation, routeProgress, stepPoints);
    } else {
      location = rawLocation;
    }
    return location;
  }

  /**
   * Increases the step index in {@link NavigationIndices} by 1.
   * <p>
   * Decodes the step points for the new step and clears the distances from
   * maneuver stack, as the maneuver has now changed.
   *
   * @param directionsRoute to get the next {@link LegStep#geometry()}
   * @param recentDistances should be cleared as a result of advancing the index
   */
  private void advanceStepIndex(DirectionsRoute directionsRoute, RingBuffer recentDistances) {
    // First increase the indices and then update the majority of information for the new routeProgress
    indices = increaseIndex(previousRouteProgress, indices);

    // First increase the indices and then update the majority of information for the new
    stepPoints = decodeStepPoints(directionsRoute, indices.legIndex(), indices.stepIndex());

    // Clear any recent distances from the maneuver (maneuver has now changed)
    if (!recentDistances.isEmpty()) {
      recentDistances.clear();
    }
  }

  /**
   * Given the current {@link DirectionsRoute} and leg / step index,
   * return a list of {@link Point} representing the current step.
   * <p>
   * This method is only used on a per-step basis as {@link PolylineUtils#decode(String, int)}
   * can be a heavy operation based on the length of the step.
   *
   * @param directionsRoute for list of steps
   * @param legIndex        to get current step list
   * @param stepIndex       to get current step
   * @return list of {@link Point} representing the current step
   */
  private List<Point> decodeStepPoints(DirectionsRoute directionsRoute, int legIndex, int stepIndex) {
    // Check for valid legs
    List<RouteLeg> legs = directionsRoute.legs();
    if (legs == null || legs.isEmpty()) {
      return stepPoints;
    }
    // Check for valid steps
    List<LegStep> steps = legs.get(legIndex).steps();
    if (steps == null || steps.isEmpty()) {
      return stepPoints;
    }

    String stepGeometry = steps.get(stepIndex).geometry();
    if (stepGeometry != null) {
      return PolylineUtils.decode(stepGeometry, PRECISION_6);
    }
    return stepPoints;
  }

  /**
   * Checks if the route provided is a new route.  If it is, all {@link RouteProgress}
   * data and {@link NavigationIndices} needs to be reset.
   *
   * @param directionsRoute to check against the current route
   */
  private void checkNewRoute(DirectionsRoute directionsRoute) {
    if (RouteUtils.isNewRoute(previousRouteProgress, directionsRoute)) {
      // Decode the first steps geometry and hold onto the resulting Position objects till the users
      // on the next step. Indices are both 0 since the user just started on the new route.
      stepPoints = decodeStepPoints(directionsRoute, 0, 0);

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

  /**
   * Given a location update, calculate the current step distance remaining.
   *
   * @param location        for current coordinates
   * @param directionsRoute for current {@link LegStep}
   * @return distance remaining in meters
   */
  private double calculateStepDistanceRemaining(Location location, DirectionsRoute directionsRoute) {
    Point snappedPosition = userSnappedToRoutePosition(location, stepPoints);
    return stepDistanceRemaining(
      snappedPosition, indices.legIndex(), indices.stepIndex(), directionsRoute, stepPoints
    );
  }

  /**
   * Callbacks for posting back to the Navigation Service once the thread finishes calculations.
   * <p>
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

package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.bearingMatchesManeuverFinalHeading;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.checkMilestones;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.getSnappedLocation;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.increaseIndex;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.isUserOffRoute;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.legDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.routeDistanceRemaining;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.stepDistanceRemaining;
import static com.mapbox.services.constants.Constants.PRECISION_6;

/**
 * This class extends handler thread to run most of the navigation calculations on a separate
 * background thread.
 */
class NavigationEngine extends HandlerThread implements Handler.Callback {

  private static final String THREAD_NAME = "NavThread";
  private RouteProgress routeProgress;
  private Handler responseHandler;
  private Handler workerHandler;
  private Callback callback;

  NavigationEngine(Handler responseHandler, Callback callback) {
    super(THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND);
    this.responseHandler = responseHandler;
    this.callback = callback;
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
    generateNewRouteProgress(
      newLocationModel.mapboxNavigation(), newLocationModel.location(),
      newLocationModel.recentDistancesFromManeuverInMeters());
    final List<Milestone> milestones = checkMilestones(
      this.routeProgress, routeProgress, newLocationModel.mapboxNavigation());
    final boolean userOffRoute = isUserOffRoute(newLocationModel, routeProgress);
    final Location location = !userOffRoute && newLocationModel.mapboxNavigation().options().snapToRoute()
      ? getSnappedLocation(newLocationModel.mapboxNavigation(), newLocationModel.location(),
      routeProgress)
      : newLocationModel.location();

    responseHandler.post(new Runnable() {
      @Override
      public void run() {
        callback.onNewRouteProgress(location, routeProgress);
        callback.onMilestoneTrigger(milestones, routeProgress);
        callback.onUserOffRoute(location, userOffRoute);
      }
    });
  }

  private void generateNewRouteProgress(MapboxNavigation mapboxNavigation, Location location,
                                        RingBuffer recentDistances) {
    DirectionsRoute directionsRoute = mapboxNavigation.getRoute();
    MapboxNavigationOptions options = mapboxNavigation.options();

    if (RouteUtils.isNewRoute(routeProgress, directionsRoute)) {
      routeProgress = RouteProgress.builder()
        .stepDistanceRemaining(directionsRoute.legs().get(0).steps().get(0).distance())
        .legDistanceRemaining(directionsRoute.legs().get(0).distance())
        .distanceRemaining(directionsRoute.distance())
        .directionsRoute(directionsRoute)
        .currentStepCoordinates(PolylineUtils.decode(
          directionsRoute.legs().get(0).steps().get(0).geometry(), PRECISION_6))
        .stepIndex(0)
        .legIndex(0)
        .build();

      if (routeProgress.currentLegProgress().upComingStep() != null) {
        routeProgress = routeProgress.toBuilder().upcomingStepCoordinates(PolylineUtils.decode(
          routeProgress.currentLegProgress().upComingStep().geometry(), PRECISION_6)).build();
      }
    }

    Point snappedPoint = RouteUtils.userSnappedToRoutePoint(location, routeProgress);
    double stepDistanceRemaining = stepDistanceRemaining(snappedPoint, routeProgress);

    // User is on a new step
    if (bearingMatchesManeuverFinalHeading(location, routeProgress,
      options.maxTurnCompletionOffset()) && stepDistanceRemaining < options.maneuverZoneRadius()) {

      // First increase the indices and then update the majority of information for the new
      // routeProgress.
      NavigationIndices indices = increaseIndex(routeProgress);

      routeProgress = routeProgress.toBuilder()
        .legIndex(indices.legIndex())
        .stepIndex(indices.stepIndex())
        .build();

      routeProgress = routeProgress.toBuilder()
        .priorStepCoordinates(routeProgress.currentStepCoordinates())
        .currentStepCoordinates(routeProgress.upcomingStepCoordinates())
        .build();

      // recalculate the stepDistance value since it has changed
      stepDistanceRemaining = stepDistanceRemaining(snappedPoint, routeProgress);

      // Remove all distance values from recentDistancesFromManeuverInMeters
      recentDistances.clear();
    }

    double legDistanceRemaining = legDistanceRemaining(stepDistanceRemaining, routeProgress);
    double routeDistanceRemaining = routeDistanceRemaining(legDistanceRemaining, routeProgress);

    routeProgress = routeProgress.toBuilder()
      .stepDistanceRemaining(stepDistanceRemaining)
      .legDistanceRemaining(legDistanceRemaining)
      .distanceRemaining(routeDistanceRemaining)
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
  }
}

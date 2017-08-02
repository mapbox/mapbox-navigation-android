package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;

import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.*;

/**
 * This class extends handler thread to run most of the navigation calculations on a separate
 * background thread.
 */
class NavigationEngine extends HandlerThread implements Handler.Callback {

  private static final String THREAD_NAME = "NavThread";

  private RouteProgress previousRouteProgress;
  private NavigationIndices indices;
  private Handler responseHandler;
  private Handler workerHandler;
  private Callback callback;

  NavigationEngine(Handler responseHandler, Callback callback) {
    super(THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND);
    this.responseHandler = responseHandler;
    this.callback = callback;
  }

  void queueTask(int msgIdentifier, NewLocationModel newLocationModel) {
    workerHandler.obtainMessage(msgIdentifier, newLocationModel)
      .sendToTarget();
  }

  void prepareHandler() {
    workerHandler = new Handler(getLooper(), this);
  }

  @Override
  public boolean handleMessage(Message msg) {
    NewLocationModel newLocationModel = (NewLocationModel) msg.obj;
    handleRequest(newLocationModel);
//        msg.recycle();
    return true;
  }

  private void handleRequest(final NewLocationModel newLocationModel) {
    final RouteProgress routeProgress = generateNewRouteProgress(newLocationModel.mapboxNavigation(), newLocationModel.location());
    final List<Milestone> milestones = checkMilestones(routeProgress, newLocationModel.mapboxNavigation());
    final boolean userOffRoute = isUserOffRoute(newLocationModel, routeProgress);
    final Location location = !userOffRoute && newLocationModel.mapboxNavigation().options().snapToRoute() ?
      getSnappedLocation(newLocationModel.mapboxNavigation(), newLocationModel.location(), routeProgress)
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

  private RouteProgress generateNewRouteProgress(MapboxNavigation mapboxNavigation, Location location) {
    DirectionsRoute directionsRoute = mapboxNavigation.getRoute();
    MapboxNavigationOptions options = mapboxNavigation.options();

    if (previousRouteProgress == null) {

      previousRouteProgress = RouteProgress.builder()
        .stepDistanceRemaining(directionsRoute.getLegs().get(0).getSteps().get(0).getDistance())
        .legDistanceRemaining(directionsRoute.getLegs().get(0).getDistance())
        .distanceRemaining(directionsRoute.getDistance())
        .directionsRoute(directionsRoute)
        .stepIndex(0)
        .legIndex(0)
        .location(location)
        .build();

      indices = NavigationIndices.create(0, 0);
    }

    if (!TextUtils.equals(directionsRoute.getGeometry(), previousRouteProgress.directionsRoute().getGeometry())) {
      resetRouteProgress();
    }

    Position snappedPosition = userSnappedToRoutePosition(location, indices.legIndex(), indices.stepIndex(), directionsRoute);
    double stepDistanceRemaining = getStepDistanceRemaining(snappedPosition, indices.legIndex(), indices.stepIndex(), directionsRoute);
    double legDistanceRemaining = getLegDistanceRemaining(stepDistanceRemaining, indices.legIndex(), indices.stepIndex(), directionsRoute);
    double routeDistanceRemaining = getRouteDistanceRemaining(legDistanceRemaining, indices.legIndex(), directionsRoute);

    if (bearingMatchesManeuverFinalHeading(location, previousRouteProgress, options.maxTurnCompletionOffset())
      && stepDistanceRemaining < options.maneuverZoneRadius()) {
      indices = increaseIndex(previousRouteProgress, indices);
    }

    // Create a RouteProgress.create object using the latest user location
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(stepDistanceRemaining)
      .legDistanceRemaining(legDistanceRemaining)
      .distanceRemaining(routeDistanceRemaining)
      .directionsRoute(directionsRoute)
      .stepIndex(indices.stepIndex())
      .legIndex(indices.legIndex())
      .location(location)
      .build();


    previousRouteProgress = routeProgress;
    return routeProgress;
  }

  private List<Milestone> checkMilestones(RouteProgress routeProgress, MapboxNavigation mapboxNavigation) {
    List<Milestone> milestones = new ArrayList<>();
    for (Milestone milestone : mapboxNavigation.getMilestones()) {
      if (milestone.isOccurring(previousRouteProgress, routeProgress)) {
        milestones.add(milestone);
      }
    }
    return milestones;
  }

  private void resetRouteProgress() {
    indices = NavigationIndices.create(0, 0);
  }

  interface Callback {
    void onNewRouteProgress(Location location, RouteProgress routeProgress);

    void onMilestoneTrigger(List<Milestone> triggeredMilestones, RouteProgress routeProgress);

    void onUserOffRoute(Location location, boolean userOffRoute);
  }
}

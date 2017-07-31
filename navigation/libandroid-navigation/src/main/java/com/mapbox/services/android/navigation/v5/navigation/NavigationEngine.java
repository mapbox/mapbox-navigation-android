package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.snap.Snap;
import com.mapbox.services.android.telemetry.utils.MathUtils;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * This class extends handler thread to run most of the navigation calculations on a separate
 * background thread.
 */
class NavigationEngine extends HandlerThread implements Handler.Callback {

  private RouteProgress previousRouteProgress;
  private Location previousLocation;
  private Handler responseHandler;
  private Handler workerHandler;
  private Callback callback;
  private int stepIndex;
  private int legIndex;

  NavigationEngine(String name, int priority, Handler responseHandler, Callback callback) {
    super(name, priority);
    this.responseHandler = responseHandler;
    this.callback = callback;
    stepIndex = 0;
    legIndex = 0;
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

  private boolean validLocationUpdate(Location location) {
    // If the locations the same as previous, no need to recalculate things
    if (location.equals(previousLocation) || location.getSpeed() <= 0) {
      return false;
    }
    // TODO filter out terrible location accuracy
    return true;
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
    }

    if (!TextUtils.equals(directionsRoute.getGeometry(), previousRouteProgress.directionsRoute().getGeometry())) {
      resetRouteProgress();
    }

    if (!validLocationUpdate(location)) {
      return previousRouteProgress;
    }

    previousLocation = location;

    Position snappedPosition = NavigationHelper.userSnappedToRoutePosition(location, legIndex, stepIndex, directionsRoute);
    double stepDistanceRemaining = NavigationHelper.getStepDistanceRemaining(snappedPosition, legIndex, stepIndex, directionsRoute);
    double legDistanceRemaining = NavigationHelper.getLegDistanceRemaining(stepDistanceRemaining, legIndex, stepIndex, directionsRoute);
    double routeDistanceRemaining = NavigationHelper.getRouteDistanceRemaining(legDistanceRemaining, legIndex, directionsRoute);

//    int[] indexes = new int[2];
//    indexes[0] = previousRouteProgress.legIndex();
//    indexes[1] = previousRouteProgress.currentLegProgress().stepIndex();
    if (bearingMatchesManeuverFinalHeading(location, previousRouteProgress, options.maxTurnCompletionOffset())
      && stepDistanceRemaining < options.maneuverZoneRadius()) {
      increaseIndex(previousRouteProgress);
    }

    // Create a RouteProgress.create object using the latest user location
    RouteProgress routeProgress = RouteProgress.builder()
      .stepDistanceRemaining(stepDistanceRemaining)
      .legDistanceRemaining(legDistanceRemaining)
      .distanceRemaining(routeDistanceRemaining)
      .directionsRoute(directionsRoute)
      .stepIndex(stepIndex)
      .legIndex(legIndex)
      .location(location)
      .build();


    previousRouteProgress = routeProgress;
    return routeProgress;
  }

  private void increaseIndex(RouteProgress routeProgress) {
    // Check if we are in the last step in the current routeLeg and iterate it if needed.
    if (stepIndex >= routeProgress.directionsRoute().getLegs().get(routeProgress.legIndex()).getSteps().size() - 2
      && legIndex < routeProgress.directionsRoute().getLegs().size() - 1) {
      legIndex += 1;
      stepIndex = 0;
    } else {
      stepIndex += 1;
    }
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

  private Location getSnappedLocation(MapboxNavigation mapboxNavigation, Location location, RouteProgress routeProgress) {
    Snap snap = mapboxNavigation.getSnapEngine();
    return snap.getSnappedLocation(location, routeProgress);
  }

  private boolean isUserOffRoute(NewLocationModel newLocationModel, RouteProgress routeProgress) {
    OffRoute offRoute = newLocationModel.mapboxNavigation().getOffRouteEngine();
    return offRoute.isUserOffRoute(newLocationModel.location(), routeProgress,
      newLocationModel.mapboxNavigation().options());
  }

  /**
   * Checks whether the user's bearing matches the next step's maneuver provided bearingAfter variable. This is one of
   * the criteria's required for the user location to be recognized as being on the next step or potentially arriving.
   *
   * @param userLocation  the location of the user
   * @param routeProgress used for getting route information
   * @return boolean true if the user location matches (using a tolerance) the final heading
   * @since 0.2.0
   */
  private static boolean bearingMatchesManeuverFinalHeading(Location userLocation, RouteProgress routeProgress,
                                                            double maxTurnCompletionOffset) {
    if (routeProgress.currentLegProgress().upComingStep() == null) {
      return false;
    }

    // Bearings need to be normalized so when the bearingAfter is 359 and the user heading is 1, we count this as
    // within the MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION.
    double finalHeading = routeProgress.currentLegProgress().upComingStep().getManeuver().getBearingAfter();
    double finalHeadingNormalized = MathUtils.wrap(finalHeading, 0, 360);
    double userHeadingNormalized = MathUtils.wrap(userLocation.getBearing(), 0, 360);
    return MathUtils.differenceBetweenAngles(finalHeadingNormalized, userHeadingNormalized)
      <= maxTurnCompletionOffset;
  }

  private void resetRouteProgress() {
    legIndex = 0;
    stepIndex = 0;
  }

  interface Callback {
    void onNewRouteProgress(Location location, RouteProgress routeProgress);

    void onMilestoneTrigger(List<Milestone> triggeredMilestones, RouteProgress routeProgress);

    void onUserOffRoute(Location location, boolean userOffRoute);
  }
}

package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.RouteState;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.snap.Snap;
import com.mapbox.services.android.navigation.v5.snap.SnapToRoute;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class RouteProcessorRunnable implements Runnable {

  private static final int ONE_SECOND_IN_MILLISECONDS = 1000;
  private static final int ARRIVAL_ZONE_RADIUS = 40;
  private final NavigationRouteProcessor routeProcessor;
  private final MapboxNavigation navigation;
  private final Handler workerHandler;
  private final Handler responseHandler;
  private final RouteProcessorBackgroundThread.Listener listener;
  private Location rawLocation;

  RouteProcessorRunnable(NavigationRouteProcessor routeProcessor,
                         MapboxNavigation navigation,
                         Handler workerHandler,
                         Handler responseHandler,
                         RouteProcessorBackgroundThread.Listener listener) {
    this.routeProcessor = routeProcessor;
    this.navigation = navigation;
    this.workerHandler = workerHandler;
    this.responseHandler = responseHandler;
    this.listener = listener;
  }

  @Override
  public void run() {
    process();
  }

  void updateRawLocation(Location rawLocation) {
    this.rawLocation = rawLocation;
  }

  private void process() {
    MapboxNavigator mapboxNavigator = navigation.retrieveMapboxNavigator();
    MapboxNavigationOptions options = navigation.options();
    DirectionsRoute route = navigation.getRoute();

    Date date = new Date();
    NavigationStatus status = mapboxNavigator.retrieveStatus(date,
      options.navigationLocationEngineIntervalLagInMilliseconds());
    NavigationStatus previousStatus = routeProcessor.retrievePreviousStatus();
    status = checkForNewLegIndex(mapboxNavigator, route, status, previousStatus, options.enableAutoIncrementLegIndex());
    RouteProgress routeProgress = routeProcessor.buildNewRouteProgress(mapboxNavigator, status, route);

    RouteRefresher routeRefresher = navigation.retrieveRouteRefresher();
    if (routeRefresher != null && routeRefresher.check(date)) {
      routeRefresher.refresh(routeProgress);
    }

    NavigationEngineFactory engineFactory = navigation.retrieveEngineFactory();
    final boolean userOffRoute = isUserOffRoute(options, status, rawLocation, routeProgress, engineFactory);
    final Location snappedLocation = findSnappedLocation(status, rawLocation, routeProgress, engineFactory);
    final boolean checkFasterRoute = checkFasterRoute(options, snappedLocation, routeProgress, engineFactory,
      userOffRoute);
    final List<Milestone> milestones = findTriggeredMilestones(navigation, routeProgress);

    sendUpdateToResponseHandler(userOffRoute, milestones, snappedLocation, checkFasterRoute, routeProgress);
    routeProcessor.updatePreviousRouteProgress(routeProgress);
    workerHandler.postDelayed(this, ONE_SECOND_IN_MILLISECONDS);
  }

  private NavigationStatus checkForNewLegIndex(MapboxNavigator mapboxNavigator, DirectionsRoute route,
                                               NavigationStatus currentStatus, NavigationStatus previousStatus,
                                               boolean autoIncrementEnabled) {
    if (previousStatus == null) {
      return currentStatus;
    }
    RouteState previousState = previousStatus.getRouteState();
    int previousLegIndex = previousStatus.getLegIndex();
    int routeLegsSize = route.legs().size() - 1;
    boolean canUpdateLeg = previousState == RouteState.COMPLETE && previousLegIndex < routeLegsSize;
    boolean isValidDistanceRemaining = previousStatus.getRemainingLegDistance() < ARRIVAL_ZONE_RADIUS;
    if (autoIncrementEnabled && (canUpdateLeg && isValidDistanceRemaining)) {
      int newLegIndex = previousLegIndex + 1;
      return mapboxNavigator.updateLegIndex(newLegIndex);
    }
    return currentStatus;
  }

  private boolean isUserOffRoute(MapboxNavigationOptions options, NavigationStatus status, Location rawLocation,
                                 RouteProgress routeProgress, NavigationEngineFactory engineFactory) {
    OffRoute offRoute = engineFactory.retrieveOffRouteEngine();
    if (offRoute instanceof OffRouteDetector) {
      return ((OffRouteDetector) offRoute).isUserOffRouteWith(status);
    }
    return offRoute.isUserOffRoute(rawLocation, routeProgress, options);
  }

  private Location findSnappedLocation(NavigationStatus status, Location rawLocation, RouteProgress routeProgress,
                                       NavigationEngineFactory engineFactory) {
    Snap snap = engineFactory.retrieveSnapEngine();
    if (snap instanceof SnapToRoute) {
      return ((SnapToRoute) snap).getSnappedLocationWith(status, rawLocation);
    }
    return snap.getSnappedLocation(rawLocation, routeProgress);
  }

  private boolean checkFasterRoute(MapboxNavigationOptions options, Location rawLocation, RouteProgress routeProgress,
                                   NavigationEngineFactory engineFactory, boolean userOffRoute) {
    FasterRoute fasterRoute = engineFactory.retrieveFasterRouteEngine();
    boolean fasterRouteDetectionEnabled = options.enableFasterRouteDetection();
    return fasterRouteDetectionEnabled
      && !userOffRoute
      && fasterRoute.shouldCheckFasterRoute(rawLocation, routeProgress);
  }

  private List<Milestone> findTriggeredMilestones(MapboxNavigation mapboxNavigation, RouteProgress routeProgress) {
    RouteProgress previousRouteProgress = routeProcessor.retrievePreviousRouteProgress();
    if (previousRouteProgress == null) {
      previousRouteProgress = routeProgress;
    }
    List<Milestone> milestones = new ArrayList<>();
    for (Milestone milestone : mapboxNavigation.getMilestones()) {
      if (milestone.isOccurring(previousRouteProgress, routeProgress)) {
        milestones.add(milestone);
      }
    }
    return milestones;
  }

  private void sendUpdateToResponseHandler(final boolean userOffRoute, final List<Milestone> milestones,
                                           final Location location, final boolean checkFasterRoute,
                                           final RouteProgress finalRouteProgress) {
    responseHandler.post(new Runnable() {
      @Override
      public void run() {
        listener.onNewRouteProgress(location, finalRouteProgress);
        listener.onMilestoneTrigger(milestones, finalRouteProgress);
        listener.onUserOffRoute(location, userOffRoute);
        listener.onCheckFasterRoute(location, finalRouteProgress, checkFasterRoute);
      }
    });
  }
}

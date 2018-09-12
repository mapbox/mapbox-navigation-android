package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.NavigationStatus;
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

import timber.log.Timber;

class RouteProcessorRunnable implements Runnable {

  private static final int ONE_SECOND = 1000;
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
    Timber.d("NAV_DEBUG Processor Runnable fired - processing...");

    SynchronizedNavigator synchronizedNavigator = navigation.retrieveSynchronizedNavigator();
    MapboxNavigationOptions options = navigation.options();
    DirectionsRoute route = navigation.getRoute();

    NavigationStatus status = synchronizedNavigator.getStatus(new Date());
    RouteProgress routeProgress = routeProcessor.buildNewRouteProgress(status, route);

    NavigationEngineFactory engineFactory = navigation.retrieveEngineFactory();
    final boolean userOffRoute = isUserOffRoute(options, status, rawLocation, routeProgress, engineFactory);
    final Location snappedLocation = findSnappedLocation(status, rawLocation, routeProgress, engineFactory);
    final boolean checkFasterRoute = checkFasterRoute(options, rawLocation, routeProgress, engineFactory, userOffRoute);
    final List<Milestone> milestones = findTriggeredMilestones(navigation, routeProgress);

    workerHandler.postDelayed(this, ONE_SECOND);
    sendUpdateToResponseHandler(userOffRoute, milestones, snappedLocation, checkFasterRoute, routeProgress);
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
      return ((SnapToRoute) snap).getSnappedLocationWith(rawLocation, status);
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

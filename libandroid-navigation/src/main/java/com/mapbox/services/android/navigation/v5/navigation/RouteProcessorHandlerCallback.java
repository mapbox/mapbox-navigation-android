package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.Message;

import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.checkMilestones;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.shouldCheckFasterRoute;

class RouteProcessorHandlerCallback implements Handler.Callback {

  private NavigationRouteProcessor routeProcessor;
  private RouteProcessorBackgroundThread.Listener listener;
  private Handler responseHandler;

  RouteProcessorHandlerCallback(NavigationRouteProcessor routeProcessor, Handler responseHandler,
                                RouteProcessorBackgroundThread.Listener listener) {
    this.routeProcessor = routeProcessor;
    this.responseHandler = responseHandler;
    this.listener = listener;
  }

  @Override
  public boolean handleMessage(Message msg) {
    NavigationLocationUpdate update = ((NavigationLocationUpdate) msg.obj);
    handleRequest(update);
    return true;
  }

  private void handleRequest(final NavigationLocationUpdate update) {
    final MapboxNavigation mapboxNavigation = update.mapboxNavigation();
    final MapboxNavigationOptions options = mapboxNavigation.options();
    final Location rawLocation = update.location();
    RouteProgress routeProgress = routeProcessor.buildNewRouteProgress(mapboxNavigation.getRoute(), rawLocation);

    NavigationEngineFactory engineFactory = mapboxNavigation.retrieveEngineFactory();
    final boolean userOffRoute = engineFactory.retrieveOffRouteEngine().isUserOffRoute(rawLocation, routeProgress, options);
    final Location location = engineFactory.retrieveSnapEngine().getSnappedLocation(rawLocation, routeProgress);

    final boolean checkFasterRoute = findFasterRoute(update, mapboxNavigation, routeProgress, userOffRoute);
    final List<Milestone> milestones = findTriggeredMilestones(mapboxNavigation, routeProgress);

    sendUpdateToListener(userOffRoute, milestones, location, checkFasterRoute, routeProgress);
  }

  private List<Milestone> findTriggeredMilestones(MapboxNavigation mapboxNavigation, RouteProgress routeProgress) {
    RouteProgress previousRouteProgress = routeProcessor.retrievePreviousRouteProgress();
    if (previousRouteProgress == null) {
      previousRouteProgress = routeProgress;
    }
    return checkMilestones(previousRouteProgress, routeProgress, mapboxNavigation);
  }

  private boolean findFasterRoute(NavigationLocationUpdate navigationLocationUpdate, MapboxNavigation mapboxNavigation,
                                  RouteProgress routeProgress, boolean userOffRoute) {
    boolean fasterRouteEnabled = mapboxNavigation.options().enableFasterRouteDetection();
    return fasterRouteEnabled && !userOffRoute
      && shouldCheckFasterRoute(navigationLocationUpdate, routeProgress);
  }

  private void sendUpdateToListener(final boolean userOffRoute, final List<Milestone> milestones,
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

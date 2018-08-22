package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

import timber.log.Timber;

/**
 * This class extends handler thread to run most of the navigation calculations on a separate
 * background thread.
 */
class RouteProcessorBackgroundThread extends HandlerThread {

  private static final String MAPBOX_NAVIGATION_THREAD_NAME = "mapbox_navigation_thread";
  private final MapboxNavigation navigation;
  private final Handler responseHandler;
  private final Listener listener;
  private final NavigationRouteProcessor routeProcessor;
  private Handler workerHandler;
  private Location unfilteredLocation;

  RouteProcessorBackgroundThread(MapboxNavigation navigation, Handler responseHandler, Listener listener) {
    super(MAPBOX_NAVIGATION_THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND);
    this.navigation = navigation;
    this.responseHandler = responseHandler;
    this.listener = listener;
    this.routeProcessor = new NavigationRouteProcessor(navigation.retrieveNavigator());
  }

  @Override
  public synchronized void start() {
    super.start();
    Timber.d("NAV_DEBUG *background thread running*");
    if (workerHandler == null) {
      workerHandler = new Handler(getLooper());
    }

    NavigationLocationUpdate locationUpdate = NavigationLocationUpdate.create(unfilteredLocation, navigation);
    RouteProcessorRunnable runnable = new RouteProcessorRunnable(
      routeProcessor, locationUpdate, workerHandler, responseHandler, listener
    );
    workerHandler.post(runnable);
  }

  void updateLocation(Location location) {
    Timber.d("NAV_DEBUG background thread Location updated");
    unfilteredLocation = location;
  }

  /**
   * Listener for posting back to the Navigation Service once the thread finishes calculations.
   * <p>
   * No matter what, with each new message added to the queue, these callbacks get invoked once
   * finished and within Navigation Service it is determined if the public corresponding listeners
   * need invoking or not; the Navigation event dispatcher class handles those callbacks.
   */
  interface Listener {

    void onNewRouteProgress(Location location, RouteProgress routeProgress);

    void onMilestoneTrigger(List<Milestone> triggeredMilestones, RouteProgress routeProgress);

    void onUserOffRoute(Location location, boolean userOffRoute);

    void onCheckFasterRoute(Location location, RouteProgress routeProgress, boolean checkFasterRoute);
  }
}

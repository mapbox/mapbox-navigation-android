package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

/**
 * This class extends handler thread to run most of the navigation calculations on a separate
 * background thread.
 */
class RouteProcessorBackgroundThread extends HandlerThread {

  private static final String MAPBOX_NAVIGATION_THREAD_NAME = "mapbox_navigation_thread";
  private static final int MSG_LOCATION_UPDATED = 1001;
  private Handler workerHandler;

  RouteProcessorBackgroundThread(Handler responseHandler, Listener listener) {
    super(MAPBOX_NAVIGATION_THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND);
    start();
    initialize(responseHandler, listener);
  }

  void queueUpdate(NavigationLocationUpdate navigationLocationUpdate) {
    workerHandler.obtainMessage(MSG_LOCATION_UPDATED, navigationLocationUpdate).sendToTarget();
  }

  private void initialize(Handler responseHandler, Listener listener) {
    NavigationRouteProcessor routeProcessor = new NavigationRouteProcessor();
    workerHandler = new Handler(getLooper(), new RouteProcessorHandlerCallback(
      routeProcessor, responseHandler, listener)
    );
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

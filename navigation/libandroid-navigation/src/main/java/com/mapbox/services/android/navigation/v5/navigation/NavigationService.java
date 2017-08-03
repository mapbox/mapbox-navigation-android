package com.mapbox.services.android.navigation.v5.navigation;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;

import java.util.List;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.buildInstructionString;

public class NavigationService extends Service implements LocationEngineListener,
  NavigationEngine.Callback {

  private static final int MSG_LOCATION_UPDATED = 1001;

  private final IBinder localBinder = new LocalBinder();
  private long timeIntervalSinceLastOffRoute;
  private MapboxNavigation mapboxNavigation;
  private LocationEngine locationEngine;
  private NavigationEngine thread;
  private NavigationNotification notificationManager;

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return localBinder;
  }

  @Override
  public void onCreate() {
    thread = new NavigationEngine(new Handler(), this);
    thread.start();
    thread.prepareHandler();
  }

  /**
   * Only should be called once since we want the service to continue running until the navigation
   * session ends.
   */
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    endNavigation();
    super.onDestroy();
  }

  void startNavigation(MapboxNavigation mapboxNavigation) {
    this.mapboxNavigation = mapboxNavigation;
    initializeNotification();
    acquireLocationEngine();
    forceLocationUpdate();
  }

  private void initializeNotification() {
    notificationManager = new NavigationNotification(this);
    // TODO support custom notification layouts
    Notification notifyBuilder = notificationManager.buildPersistentNotification(R.layout.layout_notification_default);
    startForeground(NAVIGATION_NOTIFICATION_ID, notifyBuilder);
  }

  void endNavigation() {
    locationEngine.removeLocationEngineListener(this);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      thread.quitSafely();
    } else {
      thread.quit();
    }
    stopSelf();
  }

  // TODO check changing locationEngine while service and nav sessions running.

  /**
   * location engine already checks if the listener isn't already added so no need to check here.
   */
  void acquireLocationEngine() {
    locationEngine = mapboxNavigation.getLocationEngine();
    locationEngine.addLocationEngineListener(this);
  }

  @SuppressLint("MissingPermission")
  private void forceLocationUpdate() {
    Location lastLocation = locationEngine.getLastLocation();
    if (lastLocation != null) {
      thread.queueTask(MSG_LOCATION_UPDATED, NewLocationModel.create(lastLocation, mapboxNavigation));
    }
  }

  @Override
  @SuppressLint("MissingPermission")
  public void onConnected() {
    Timber.d("NavigationService now connected to location listener.");
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    Timber.d("onLocationChanged");
    if (location != null) {
      if (validLocationUpdate(location)) {
        thread.queueTask(MSG_LOCATION_UPDATED, NewLocationModel.create(location, mapboxNavigation));
      }
    }
  }

  private boolean validLocationUpdate(Location location) {
    if (locationEngine.getLastLocation() == null) {
      return true;
    }
    // TODO check that the location has speed
    // TODO fix mock location engine last location
    // If the locations the same as previous, no need to recalculate things
//    if (location.equals(locationEngine.getLastLocation())
//      || (location.getSpeed() <= 0 /*&& location.hasSpeed()*/)) {
//      return false;
//    }
    // TODO filter out terrible location accuracy
    return true;
  }

  @Override
  public void onNewRouteProgress(Location location, RouteProgress routeProgress) {
    notificationManager.updateDefaultNotification(routeProgress);
    mapboxNavigation.getEventDispatcher().onProgressChange(location, routeProgress);
  }

  @Override
  public void onMilestoneTrigger(List<Milestone> triggeredMilestones, RouteProgress routeProgress) {
    for (Milestone milestone : triggeredMilestones) {
      String instruction = buildInstructionString(routeProgress, milestone);
      mapboxNavigation.getEventDispatcher().onMilestoneEvent(routeProgress, instruction, milestone.getIdentifier());
    }
  }

  @Override
  public void onUserOffRoute(Location location, boolean userOffRoute) {
    if (userOffRoute) {
      if (location.getTime() > timeIntervalSinceLastOffRoute
        + TimeUnit.SECONDS.toMillis(mapboxNavigation.options().secondsBeforeReroute())) {
        mapboxNavigation.getEventDispatcher().onUserOffRoute(location);
        timeIntervalSinceLastOffRoute = location.getTime();
      }
    } else {
      timeIntervalSinceLastOffRoute = location.getTime();
    }
  }

  class LocalBinder extends Binder {
    NavigationService getService() {
      Timber.d("Local binder called.");
      return NavigationService.this;
    }
  }
}

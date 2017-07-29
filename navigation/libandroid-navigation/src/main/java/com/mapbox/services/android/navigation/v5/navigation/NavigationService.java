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
import android.os.Process;
import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;

import java.util.List;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class NavigationService extends Service implements LocationEngineListener, NavigationEngine.Callback {

  private static final int ONGOING_NOTIFICATION_ID = 1;
  private static final int MSG_LOCATION_UPDATED = 1001;

  private final IBinder localBinder = new LocalBinder();
  private long timeIntervalSinceLastOffRoute;
  private MapboxNavigation mapboxNavigation;
  private LocationEngine locationEngine;
  private Notification notifyBuilder;
  private NavigationEngine thread;

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return localBinder;
  }

  @Override
  public void onCreate() {
    thread = new NavigationEngine("NavThread", Process.THREAD_PRIORITY_BACKGROUND, new Handler(), this);
    thread.start();
    thread.prepareHandler();

    NotificationManager notificationManager = new NotificationManager(this);
    notifyBuilder = notificationManager.buildPersistentNotification();
    startForeground(ONGOING_NOTIFICATION_ID, notifyBuilder);
  }

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
    acquireLocationEngine();
    forceLocationUpdate();
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
    thread.queueTask(MSG_LOCATION_UPDATED, NewLocationModel.create(location, mapboxNavigation));
  }

  @Override
  public void onNewRouteProgress(Location location, RouteProgress routeProgress) {
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

  private String buildInstructionString(RouteProgress routeProgress, Milestone milestone) {
    if (milestone.getInstruction() != null) {
      // Create a new custom instruction based on the Instruction packaged with the Milestone
      return milestone.getInstruction().buildInstruction(routeProgress);
    } else {
      return "";
    }
  }

  class LocalBinder extends Binder {
    NavigationService getService() {
      Timber.d("Local binder called.");
      return NavigationService.this;
    }
  }
}

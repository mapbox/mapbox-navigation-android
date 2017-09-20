package com.mapbox.services.android.navigation.v5.navigation;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.buildInstructionString;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.mapbox.services.android.location.MockLocationEngine;
import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;

import java.util.List;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * Internal usage only, use navigation by initializing a new instance of {@link MapboxNavigation}
 * and customizing the navigation experience through that class.
 * <p>
 * This class is first created and started when {@link MapboxNavigation#startNavigation(DirectionsRoute)}
 * get's called and runs in the background until either the navigation sessions ends implicitly or
 * the hosting activity gets destroyed. Location updates are also tracked and handled inside this
 * service. Thread creation gets created in this service and maintains the thread until the service
 * gets destroyed.
 * </p>
 */
public class NavigationService extends Service implements LocationEngineListener,
  NavigationEngine.Callback {

  // Message id used when a new location update occurs and we send to the thread.
  private static final int MSG_LOCATION_UPDATED = 1001;

  private final IBinder localBinder = new LocalBinder();
  private NavigationNotification notificationManager;
  private long timeIntervalSinceLastOffRoute;
  private MapboxNavigation mapboxNavigation;
  private LocationEngine locationEngine;
  private RouteProgress routeProgress;
  private boolean firstProgressUpdate = true;
  private NavigationEngine thread;
  private Location location;

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
    super.onDestroy();
    // User canceled navigation session
    if (routeProgress != null && location != null) {
      NavigationMetricsWrapper.cancelEvent(mapboxNavigation.getSessionState(), routeProgress,
        location);
    }
    endNavigation();
    if (notificationManager != null) {
      notificationManager.onDestroy();
    }
  }

  /**
   * This gets called when {@link MapboxNavigation#startNavigation(DirectionsRoute)} is called and
   * setups variables among other things on the Navigation Service side.
   */
  void startNavigation(MapboxNavigation mapboxNavigation) {
    this.mapboxNavigation = mapboxNavigation;
    if (mapboxNavigation.options().enableNotification()) {
      initializeNotification();
    }
    acquireLocationEngine();
    forceLocationUpdate();
  }

  /**
   * builds a new navigation notification instance and attaches it to this service.
   */
  private void initializeNotification() {
    notificationManager = new NavigationNotification(this, mapboxNavigation);
    Notification notifyBuilder
      = notificationManager.buildPersistentNotification(R.layout.layout_notification_default,
      R.layout.layout_notification_default_big);
    startForeground(NAVIGATION_NOTIFICATION_ID, notifyBuilder);
  }

  /**
   * Specifically removes this locationEngine listener which was added at the very beginning, quits
   * the thread, and finally stops this service from running in the background.
   */
  void endNavigation() {
    locationEngine.removeLocationEngineListener(this);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      thread.quitSafely();
    } else {
      thread.quit();
    }
  }

  /**
   * location engine already checks if the listener isn't already added so no need to check here.
   * If the user decides to call {@link MapboxNavigation#setLocationEngine(LocationEngine)} during
   * the navigation session, this gets called again in order to attach the location listener to the
   * new engine.
   */
  void acquireLocationEngine() {
    locationEngine = mapboxNavigation.getLocationEngine();
    locationEngine.addLocationEngineListener(this);
  }

  /**
   * At the very beginning of navigation session, a forced location update occurs so that the
   * developer can immediately get a routeProgress object to display information.
   */
  @SuppressWarnings("MissingPermission")
  private void forceLocationUpdate() {
    Location lastLocation = locationEngine.getLastLocation();
    if (lastLocation != null) {
      thread.queueTask(MSG_LOCATION_UPDATED, NewLocationModel.create(lastLocation, mapboxNavigation));
    }
  }

  @Override
  @SuppressWarnings("MissingPermission")
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

  /**
   * Runs several checks on the actual location object itself in order to ensure that we are
   * performing navigation progress on a accurate/valid location update.
   */
  @SuppressWarnings("MissingPermission")
  private boolean validLocationUpdate(Location location) {
    // TODO fix mock location engine and remove this if statement.
    if (locationEngine instanceof MockLocationEngine) {
      return true;
    }
    if (locationEngine.getLastLocation() == null) {
      return true;
    }
    // If the locations the same as previous, no need to recalculate things
    return !(location.equals(locationEngine.getLastLocation())
      || (location.getSpeed() <= 0 && location.hasSpeed())
      || location.getAccuracy() >= 100);
  }

  /**
   * Corresponds to ProgressChangeListener object, updating the notification and passing information
   * to the navigation event dispatcher.
   */
  @Override
  public void onNewRouteProgress(Location location, RouteProgress routeProgress) {
    this.routeProgress = routeProgress;
    this.location = location;
    if (firstProgressUpdate) {
      NavigationMetricsWrapper.departEvent(mapboxNavigation.getSessionState(), routeProgress,
        location);
      firstProgressUpdate = false;
    }
    if (mapboxNavigation.options().enableNotification()) {
      notificationManager.updateDefaultNotification(routeProgress);
    }
    mapboxNavigation.getEventDispatcher().onProgressChange(location, routeProgress);
  }

  /**
   * With each valid and successful location update, this will get called once the work on the
   * navigation engine thread has finished. Depending on whether or not a milestone gets triggered
   * or not, the navigation event dispatcher will be called to notify the developer.
   */
  @Override
  public void onMilestoneTrigger(List<Milestone> triggeredMilestones, RouteProgress routeProgress) {
    for (Milestone milestone : triggeredMilestones) {
      String instruction = buildInstructionString(routeProgress, milestone);
      mapboxNavigation.getEventDispatcher().onMilestoneEvent(
        routeProgress, instruction, milestone.getIdentifier());
    }
  }

  /**
   * With each valid and successful location update, this callback gets invoked and depending on
   * whether or not the user is off route, the event dispatcher gets called.
   */
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

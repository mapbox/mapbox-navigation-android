package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationEventDispatcher;
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationTelemetry;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;
import com.mapbox.services.android.navigation.v5.route.RouteFetcher;

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
public class NavigationService extends Service {

  private final IBinder localBinder = new LocalBinder();
  private RouteProcessorBackgroundThread thread;
  private LocationUpdater locationUpdater;
  private RouteFetcher routeFetcher;
  private NavigationNotificationProvider notificationProvider;

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return localBinder;
  }

  /**
   * Only should be called once since we want the service to continue running until the navigation
   * session ends.
   */
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    NavigationTelemetry.getInstance().initializeLifecycleMonitor(getApplication());
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    stopForeground(true);
    super.onDestroy();
  }

  /**
   * This gets called when {@link MapboxNavigation#startNavigation(DirectionsRoute)} is called and
   * setups variables among other things on the Navigation Service side.
   */
  void startNavigation(MapboxNavigation mapboxNavigation) {
    initialize(mapboxNavigation);
    startForegroundNotification(notificationProvider.retrieveNotification());
  }

  /**
   * Removes the location / route listeners and  quits the thread.
   */
  void endNavigation() {
    NavigationTelemetry.getInstance().endSession();
    routeFetcher.clearListeners();
    locationUpdater.removeLocationUpdates();
    notificationProvider.shutdown(getApplication());
    thread.quit();
  }

  void updateLocationEngine(LocationEngine locationEngine) {
    locationUpdater.updateLocationEngine(locationEngine);
  }

  void updateLocationEngineRequest(LocationEngineRequest request) {
    locationUpdater.updateLocationEngineRequest(request);
  }

  private void initialize(MapboxNavigation mapboxNavigation) {
    NavigationEventDispatcher dispatcher = mapboxNavigation.getEventDispatcher();
    String accessToken = mapboxNavigation.obtainAccessToken();
    initializeRouteFetcher(dispatcher, accessToken, mapboxNavigation.retrieveEngineFactory());
    initializeNotificationProvider(mapboxNavigation);
    initializeRouteProcessorThread(mapboxNavigation, dispatcher, routeFetcher, notificationProvider);
    initializeLocationUpdater(mapboxNavigation);
  }

  private void initializeRouteFetcher(NavigationEventDispatcher dispatcher, String accessToken,
                                      NavigationEngineFactory engineProvider) {
    FasterRoute fasterRouteEngine = engineProvider.retrieveFasterRouteEngine();
    NavigationFasterRouteListener listener = new NavigationFasterRouteListener(dispatcher, fasterRouteEngine);
    routeFetcher = new RouteFetcher(getApplication(), accessToken);
    routeFetcher.addRouteListener(listener);
  }

  private void initializeNotificationProvider(MapboxNavigation mapboxNavigation) {
    notificationProvider = new NavigationNotificationProvider(getApplication(), mapboxNavigation);
  }

  private void initializeRouteProcessorThread(MapboxNavigation mapboxNavigation,
                                              NavigationEventDispatcher dispatcher,
                                              RouteFetcher routeFetcher,
                                              NavigationNotificationProvider notificationProvider) {
    RouteProcessorThreadListener listener = new RouteProcessorThreadListener(
      dispatcher, routeFetcher, notificationProvider
    );
    thread = new RouteProcessorBackgroundThread(mapboxNavigation, new Handler(), listener);
  }

  private void initializeLocationUpdater(MapboxNavigation mapboxNavigation) {
    LocationEngine locationEngine = mapboxNavigation.getLocationEngine();
    LocationEngineRequest locationEngineRequest = mapboxNavigation.retrieveLocationEngineRequest();
    NavigationEventDispatcher dispatcher = mapboxNavigation.getEventDispatcher();
    locationUpdater = new LocationUpdater(thread, dispatcher, locationEngine, locationEngineRequest);
  }

  private void startForegroundNotification(NavigationNotification navigationNotification) {
    Notification notification = navigationNotification.getNotification();
    int notificationId = navigationNotification.getNotificationId();
    notification.flags = Notification.FLAG_FOREGROUND_SERVICE;
    startForeground(notificationId, notification);
  }

  class LocalBinder extends Binder {
    NavigationService getService() {
      Timber.d("Local binder called.");
      return NavigationService.this;
    }
  }
}

package com.mapbox.services.android.navigation.v5;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.mapbox.services.Experimental;
import com.mapbox.services.android.navigation.v5.listeners.AlertLevelChangeListener;
import com.mapbox.services.android.navigation.v5.listeners.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.listeners.OffRouteListener;
import com.mapbox.services.android.navigation.v5.listeners.ProgressChangeListener;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import timber.log.Timber;

import static com.mapbox.services.android.telemetry.location.LocationEnginePriority.BALANCED_POWER_ACCURACY;
import static com.mapbox.services.android.telemetry.location.LocationEnginePriority.HIGH_ACCURACY;

/**
 * This is an experimental API. Experimental APIs are quickly evolving and
 * might change or be removed in minor versions.
 *
 * @since 0.1.0
 */
@Experimental
public class NavigationService extends Service implements LocationEngineListener, ProgressChangeListener {
  private static final int ONGOING_NOTIFICATION_ID = 1;

  private int startId;

  private final IBinder localBinder = new LocalBinder();

  private LocationEngine locationEngine;
  private List<NavigationEventListener> navigationEventListeners;
  private List<ProgressChangeListener> progressChangeListeners;
  private List<AlertLevelChangeListener> alertLevelChangeListeners;
  private List<OffRouteListener> offRouteListeners;
  private NotificationCompat.Builder notifyBuilder;
  private RouteController routeController;
  private MapboxNavigationOptions options;
  private boolean previousUserOffRoute;
  private boolean userOffRoute;
  private boolean snapToRoute;

  private RouteProgress routeProgress;
  private DirectionsRoute directionsRoute;

  @Override
  public void onCreate() {
    super.onCreate();
    Timber.d("Navigation service created.");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Timber.d("Navigation service started.");
    this.startId = startId;
    if (navigationEventListeners != null) {
      for (NavigationEventListener navigationEventListener : navigationEventListeners) {
        navigationEventListener.onRunning(false);
      }
    }
    startNavigation();
    return Service.START_NOT_STICKY;
  }

  public void setupNotification(Activity activity) {
    Timber.d("Setting up notification.");

    // Sets up the top bar notification
    notifyBuilder = new NotificationCompat.Builder(this)
      .setContentTitle("Mapbox Navigation")
      .setContentText("navigating")
      .setSmallIcon(com.mapbox.services.android.navigation.R.drawable.ic_navigation_black_24dp)
      .setContentIntent(PendingIntent.getActivity(this, 0,
        new Intent(this, activity.getClass()), 0));

    startForeground(ONGOING_NOTIFICATION_ID, notifyBuilder.build());
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    Timber.d("Navigation service is now bound.");
    return localBinder;
  }

  @Override
  public boolean onUnbind(Intent intent) {
    Timber.d("Navigation service is now unbound.");
    return super.onUnbind(intent);
  }

  @Override
  public void onRebind(Intent intent) {
    Timber.d("Navigation service is now rebound.");
    super.onRebind(intent);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Timber.d("Navigation service destroyed.");
  }

  public class LocalBinder extends Binder {
    public NavigationService getService() {
      Timber.d("Local binder called.");
      return NavigationService.this;
    }
  }

  /*
   * Public API
   */

  public void setSnapToRoute(boolean snapToRoute) {
    this.snapToRoute = snapToRoute;
  }

  public void setOptions(MapboxNavigationOptions options) {

    this.options = options;
    routeController = new RouteController(options);
  }

  // TODO method might not be needed
  private void startNavigation() {
    Timber.d("Navigation session started.");
    if (navigationEventListeners != null) {
      for (NavigationEventListener navigationEventListener : navigationEventListeners) {
        navigationEventListener.onRunning(true);
      }
    }
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    NavigationService.this.routeProgress = routeProgress;
    // If the user arrives at the final destination, end the navigation session.
    if (routeProgress.getAlertUserLevel() == NavigationConstants.ARRIVE_ALERT_LEVEL) {
      endNavigation();
    }
  }

  public void startRoute(DirectionsRoute directionsRoute) {
    Timber.d("Start route called.");
    this.directionsRoute = directionsRoute;

    if (locationEngine != null) {
      // Begin listening into location at its highest accuracy and add navigation location listener
      locationEngine.setPriority(HIGH_ACCURACY);
      locationEngine.requestLocationUpdates();
      locationEngine.addLocationEngineListener(this);

      if (navigationEventListeners != null) {
        for (NavigationEventListener navigationEventListener : navigationEventListeners) {
          navigationEventListener.onRunning(true);
        }
      }

    } else {
      Timber.d("locationEngine null in NavigationService");
    }
  }

  public void updateRoute(DirectionsRoute directionsRoute) {
    Timber.d("Updating route");
    this.directionsRoute = directionsRoute;
  }

  public void endNavigation() {
    Timber.d("Navigation session ended.");
    if (navigationEventListeners != null) {
      for (NavigationEventListener navigationEventListener : navigationEventListeners) {
        navigationEventListener.onRunning(false);
      }
    }

    // Remove the this navigation service progress change listener
    progressChangeListeners.remove(this);

    // Lower accuracy to minimize battery usage while not in navigation mode.
    // TODO restore accuracy state to what user had before nav session
    locationEngine.setPriority(BALANCED_POWER_ACCURACY);
    locationEngine.removeLocationEngineListener(this);
    locationEngine.removeLocationUpdates();
    locationEngine.deactivate();

    // Removes the foreground notification
    stopForeground(true);

    // Stops the service
    stopSelf(startId);
  }

  public void setNavigationEventListeners(List<NavigationEventListener> navigationEventListeners) {
    this.navigationEventListeners = navigationEventListeners;
  }

  public void setAlertLevelChangeListeners(List<AlertLevelChangeListener> alertLevelChangeListeners) {
    this.alertLevelChangeListeners = alertLevelChangeListeners;
  }

  public void setProgressChangeListeners(List<ProgressChangeListener> progressChangeListeners) {
    // Add a progress listener so this service is notified when the user arrives at their destination.
    progressChangeListeners.add(this);
    this.progressChangeListeners = progressChangeListeners;
  }

  public void setOffRouteListeners(List<OffRouteListener> offRouteListeners) {
    this.offRouteListeners = offRouteListeners;
  }

  public void setLocationEngine(LocationEngine locationEngine) {
    this.locationEngine = locationEngine;
  }

  @Override
  public void onConnected() {
    Timber.d("NavigationService now connected to location listener");
    Location lastLocation = locationEngine.getLastLocation();
    if (routeController != null && lastLocation != null) {
      if (routeProgress == null) {
        Timber.d("Create new routeProgress object");
        routeProgress = new RouteProgress(
          directionsRoute,
          lastLocation,
          0, 0,
          NavigationConstants.NONE_ALERT_LEVEL
        );
      }
      updateLocation(lastLocation, directionsRoute, routeProgress);
    }
  }

  @Override
  public void onLocationChanged(Location location) {
    Timber.d("LocationChange occurred");
    if (routeProgress == null && location != null) {
      Timber.d("Create new routeProgress object");
      routeProgress = new RouteProgress(
        directionsRoute,
        location,
        0, 0,
        NavigationConstants.NONE_ALERT_LEVEL
      );
    }
    if (routeController != null && location != null) {
      updateLocation(location, directionsRoute, routeProgress);
    }
  }

  public void updateLocation(Location userLocation, DirectionsRoute directionsRoute, RouteProgress previousRouteProgress) {
    this.directionsRoute = directionsRoute;

    if (userLocation == null || directionsRoute == null) {
      return;
    }

    // With a new location update, we create a new RouteProgress object.
    RouteProgress routeProgress = RouteProgress.buildUpdatedRouteProgress(routeController, userLocation, previousRouteProgress, directionsRoute);

    userOffRoute = routeController.userIsOnRoute(userLocation, routeProgress.getCurrentLeg());

    if (snapToRoute && !userOffRoute) {
      // Pass in the snapped location with all the other location data remaining intact for their use.
      userLocation.setLatitude(routeProgress.usersCurrentSnappedPosition().getLatitude());
      userLocation.setLongitude(routeProgress.usersCurrentSnappedPosition().getLongitude());
      userLocation.setBearing(routeController.snapUserBearing(userLocation, routeProgress, snapToRoute));
    }

    // Only report user off route once.
    if (userOffRoute && (userOffRoute != previousUserOffRoute)) {
      for (OffRouteListener offRouteListener : offRouteListeners) {
        offRouteListener.userOffRoute(userLocation);
      }
      previousUserOffRoute = userOffRoute;
    }

    if (routeProgress.getCurrentLegProgress().getStepIndex()
      != previousRouteProgress.getCurrentLegProgress().getStepIndex()
      || previousRouteProgress.getAlertUserLevel() != routeProgress.getAlertUserLevel()) {

      for (AlertLevelChangeListener alertLevelChangeListener : alertLevelChangeListeners) {
        alertLevelChangeListener.onAlertLevelChange(routeProgress.getAlertUserLevel(), routeProgress);
      }
    }

    for (ProgressChangeListener progressChangeListener : progressChangeListeners) {
      progressChangeListener.onProgressChange(userLocation, routeProgress);
    }
  }
}

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
  private CopyOnWriteArrayList<NavigationEventListener> navigationEventListeners;
  private NotificationCompat.Builder notifyBuilder;
  private CopyOnWriteArrayList<ProgressChangeListener> progressChangeListeners;
  private RouteController routeController;
  private MapboxNavigationOptions options;

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
      .setContentText("Distance: " + routeProgress.getCurrentLegProgress().getCurrentStepProgress()
        .getDistanceRemaining())
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
    if(routeController != null) {
      routeController.setSnapToRoute(snapToRoute);
    }
  }

  public void setOptions(MapboxNavigationOptions options) {
    this.options = options;
  }

  private void startNavigation() {
    Timber.d("Navigation session started.");
    if (navigationEventListeners != null) {
      for (NavigationEventListener navigationEventListener : navigationEventListeners) {
        navigationEventListener.onRunning(true);
      }
    }
    routeController = new RouteController(options);
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

  public void setNavigationEventListeners(CopyOnWriteArrayList<NavigationEventListener> navigationEventListeners) {
    this.navigationEventListeners = navigationEventListeners;
  }

  public void setAlertLevelChangeListeners(CopyOnWriteArrayList<AlertLevelChangeListener> alertLevelChangeListeners) {
    routeController.setAlertLevelChangeListener(alertLevelChangeListeners);
  }

  public void setProgressChangeListeners(CopyOnWriteArrayList<ProgressChangeListener> progressChangeListeners) {
    // Add a progress listener so this service is notified when the user arrives at their destination.
    progressChangeListeners.add(this);
    routeController.setProgressChangeListener(progressChangeListeners);
    this.progressChangeListeners = progressChangeListeners;
  }

  public void setOffRouteListeners(CopyOnWriteArrayList<OffRouteListener> offRouteListeners) {
    routeController.setOffRouteListener(offRouteListeners);
  }

  public void setLocationEngine(LocationEngine locationEngine) {
    this.locationEngine = locationEngine;
  }

  @Override
  public void onConnected() {
    Timber.d("NavigationService now connected to location listener");
    Location lastLocation = locationEngine.getLastLocation();
    if (routeProgress == null) {
      Timber.d("Create new routeProgress object");
      routeProgress = new RouteProgress(
        directionsRoute,
        Position.fromCoordinates(lastLocation.getLongitude(), lastLocation.getLatitude()),
        0, 0,
        NavigationConstants.NONE_ALERT_LEVEL
      );
    }
    if (routeController != null && lastLocation != null) {
      routeController.updateLocation(lastLocation, directionsRoute, routeProgress);
    }
  }

  @Override
  public void onLocationChanged(Location location) {
    Timber.d("LocationChange occurred");
    if (routeProgress == null && location != null) {
      Timber.d("Create new routeProgress object");
      routeProgress = new RouteProgress(
        directionsRoute,
        Position.fromCoordinates(location.getLongitude(), location.getLatitude()),
        0, 0,
        NavigationConstants.NONE_ALERT_LEVEL
      );
    }
    if (routeController != null && location != null) {
      routeController.updateLocation(location, directionsRoute, routeProgress);
    }
  }
}

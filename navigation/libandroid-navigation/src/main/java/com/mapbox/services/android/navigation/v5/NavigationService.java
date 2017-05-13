package com.mapbox.services.android.navigation.v5;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
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
  private CopyOnWriteArrayList<ProgressChangeListener> progressChangeListeners;
  private NotificationCompat.Builder notifyBuilder;
  private MapboxNavigationOptions options;
  private boolean snapToRoute;
  private NavigationEngine navigationEngine;
  private DirectionsRoute directionsRoute;

  @Override
  public void onCreate() {
    super.onCreate();
    if (options == null) {
      options = new MapboxNavigationOptions();
    }
    navigationEngine = new NavigationEngine(options, snapToRoute);
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
    if (navigationEngine != null) {
      navigationEngine.setSnapEnabled(snapToRoute);
    }
  }

  public void setOptions(MapboxNavigationOptions options) {
    this.options = options;
    if (navigationEngine != null) {
      navigationEngine.setOptions(options);
    }
  }

  // TODO use this to update notification
  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    // If the user arrives at the final destination, end the navigation session.
    if (routeProgress.getAlertUserLevel() == NavigationConstants.ARRIVE_ALERT_LEVEL) {
      endNavigation();
    }
  }

  @SuppressWarnings( {"MissingPermission"})
  public void startRoute(DirectionsRoute directionsRoute) {
    this.directionsRoute = directionsRoute;
    Timber.d("Start route called.");

    if (locationEngine != null) {
      // Begin listening into location at its highest accuracy and add navigation location listener
      locationEngine.setPriority(HIGH_ACCURACY);
      locationEngine.addLocationEngineListener(this);
      locationEngine.activate();

      if (navigationEventListeners != null) {
        for (NavigationEventListener navigationEventListener : navigationEventListeners) {
          navigationEventListener.onRunning(true);
        }
      }
      if (locationEngine.getLastLocation() != null) {
        navigationEngine.onLocationChanged(directionsRoute, locationEngine.getLastLocation());
      }
    } else {
      // TODO throw exception here
      Timber.d("locationEngine null in NavigationService");
    }
  }

  public void updateRoute(DirectionsRoute directionsRoute) {
    Timber.d("Updating route");
    if (navigationEngine != null) {
      navigationEngine.setDirectionsRoute(directionsRoute);
    }
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
    if (navigationEngine != null) {
      navigationEngine.setAlertLevelChangeListeners(alertLevelChangeListeners);
    }
  }

  public void setProgressChangeListeners(CopyOnWriteArrayList<ProgressChangeListener> progressChangeListeners) {
    // Add a progress listener so this service is notified when the user arrives at their destination.
    this.progressChangeListeners = progressChangeListeners;
    progressChangeListeners.add(this);
    if (navigationEngine != null) {
      navigationEngine.setProgressChangeListeners(progressChangeListeners);
    }
  }

  public void setOffRouteListeners(CopyOnWriteArrayList<OffRouteListener> offRouteListeners) {
    if (navigationEngine != null) {
      navigationEngine.setOffRouteListeners(offRouteListeners);
    }
  }

  public void setLocationEngine(LocationEngine locationEngine) {
    this.locationEngine = locationEngine;
  }

  @SuppressWarnings( {"MissingPermission"})
  @Override
  public void onConnected() {
    Timber.d("NavigationService now connected to location listener");
    // Update location update request with newest settings.
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    if (location != null) {
      Timber.d("LocationChange occurred");
      navigationEngine.onLocationChanged(directionsRoute, location);
    }
  }
}

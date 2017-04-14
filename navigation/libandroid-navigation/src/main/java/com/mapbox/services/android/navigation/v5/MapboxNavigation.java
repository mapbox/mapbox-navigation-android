package com.mapbox.services.android.navigation.v5;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.IntRange;

import com.mapbox.services.Experimental;
import com.mapbox.services.android.location.LostLocationEngine;
import com.mapbox.services.android.navigation.v5.listeners.AlertLevelChangeListener;
import com.mapbox.services.android.navigation.v5.listeners.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.listeners.OffRouteListener;
import com.mapbox.services.android.navigation.v5.listeners.ProgressChangeListener;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.api.ServicesException;
import com.mapbox.services.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.api.directions.v5.MapboxDirections;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.commons.models.Position;

import java.util.concurrent.CopyOnWriteArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * This is an experimental API. Experimental APIs are quickly evolving and
 * might change or be removed in minor versions.
 *
 * @since 2.0.0
 */
@Experimental
public class MapboxNavigation {

  // Navigation service variables
  private NavigationServiceConnection connection;
  private NavigationService navigationService;
  private Context context;
  private boolean isBound;

  // Navigation variables
  private CopyOnWriteArrayList<AlertLevelChangeListener> alertLevelChangeListeners;
  private CopyOnWriteArrayList<NavigationEventListener> navigationEventListeners;
  private CopyOnWriteArrayList<ProgressChangeListener> progressChangeListeners;
  private CopyOnWriteArrayList<OffRouteListener> offRouteListeners;
  private LocationEngine locationEngine;
  private boolean snapToRoute;

  // Requesting route variables
  private String profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC;
  private DirectionsRoute route;
  private Position destination;
  private Integer userBearing;
  private String accessToken;
  private Position origin;

  /*
   * Constructor
   */

  public MapboxNavigation(Context context, String accessToken) {
    Timber.d("MapboxNavigation initiated.");
    this.context = context;
    this.accessToken = accessToken;
    connection = new NavigationServiceConnection();
    isBound = false;
    navigationService = null;
    snapToRoute = true;
    alertLevelChangeListeners = new CopyOnWriteArrayList<>();
    navigationEventListeners = new CopyOnWriteArrayList<>();
    progressChangeListeners = new CopyOnWriteArrayList<>();
    offRouteListeners = new CopyOnWriteArrayList<>();
  }

  /*
   * Lifecycle
   */

  /**
   * It's required to include both the {@code onStop} and {@code onStart} inside your navigation activities identically
   * named lifecycle methods.
   *
   * @since 2.0.0
   */
  public void onStart() {
    Timber.d("MapboxNavigation onStart.");

    // Only bind if the service was previously started

    context.bindService(getServiceIntent(), connection, 0);
    isBound = true;
  }

  /**
   * It's required to include both the {@code onStop} and {@code onStart} inside your navigation activities identically
   * named lifecycle methods.
   *
   * @since 2.0.0
   */
  public void onStop() {
    Timber.d("MapboxNavigation onStop.");
    if (isBound) {
      Timber.d("unbindService called");
      context.unbindService(connection);
    }
  }

  public void onDestroy() {
    Timber.d("MapboxNavigation onDestroy.");
    context.stopService(getServiceIntent());
  }

  /*
   * Navigation setup methods
   */

  /**
   * Before starting a navigationService, it is required to setup a {@link LocationEngine} and pass it into your
   * instance of {@code MapboxNavigation}. If a locationEngine isn't provided, one will be created.
   *
   * @param locationEngine A {@link LocationEngine} used for the navigation session.
   */
  public void setLocationEngine(LocationEngine locationEngine) {
    this.locationEngine = locationEngine;
  }

  /**
   * Optionally listen into when a new navigation event occurs. This listener can be used to listen into when the
   * navigation service is running in the background or not.
   *
   * @param navigationEventListener a new {@link NavigationEventListener} which will be notified when navigation events
   *                                occur.
   * @since 2.0.0
   */
  public void addNavigationEventListener(NavigationEventListener navigationEventListener) {
    if (!this.navigationEventListeners.contains(navigationEventListener)) {
      this.navigationEventListeners.add(navigationEventListener);
    }
  }

  public void removeNavigationEventListener(NavigationEventListener navigationEventListener) {
    this.navigationEventListeners.remove(navigationEventListener);
  }

  /**
   * Optionally, setup a alert change listener which will be notified when the user reaches a particular part of the
   * route. This listener is good for notifying your user they need to perform a new action.
   *
   * @param alertLevelChangeListener a new {@link AlertLevelChangeListener} which will be notified when event occurs.
   * @since 2.0.0
   */
  public void addAlertLevelChangeListener(AlertLevelChangeListener alertLevelChangeListener) {
    if (!this.alertLevelChangeListeners.contains(alertLevelChangeListener)) {
      this.alertLevelChangeListeners.add(alertLevelChangeListener);
    }
  }

  public void removeAlertLevelChangeListener(AlertLevelChangeListener alertLevelChangeListener) {
    this.alertLevelChangeListeners.remove(alertLevelChangeListener);
  }

  /**
   * Optionally, setup a progress change listener to be notified each time the users location has changed. For more
   * details see {@link ProgressChangeListener}.
   *
   * @param progressChangeListener a new {@link ProgressChangeListener} which will be notified when event occurs.
   * @since 2.0.0
   */
  public void addProgressChangeListener(ProgressChangeListener progressChangeListener) {
    if (!this.progressChangeListeners.contains(progressChangeListener)) {
      this.progressChangeListeners.add(progressChangeListener);
    }
  }

  public void removeProgressChangeListener(ProgressChangeListener progressChangeListener) {
    progressChangeListeners.remove(progressChangeListener);
  }

  public void addOffRouteListener(OffRouteListener offRouteListener) {
    if (!this.offRouteListeners.contains(offRouteListener)) {
      this.offRouteListeners.add(offRouteListener);
    }
  }

  public void removeOffRouteListener(OffRouteListener offRouteListener) {
    offRouteListeners.remove(offRouteListener);
  }

  /**
   * Disable or enable the snap to route for navigation, by default this is set to enable. When enabled, the snap to
   * route functionality of this SDK takes place and the user will snap to the route if the user is currently along the
   * route giving a better user experience. It's recommend to leave this enabled. Otherwise, the location used for
   * Navigation will be the actual GPS location which will typically be noisy and not follow the route.
   *
   * @param snapToRoute {@code boolean} true if you'd like snap to route be enabled, else false; defaults true.
   * @since 2.0.0
   */
  public void setSnapToRoute(boolean snapToRoute) {
    this.snapToRoute = snapToRoute;
  }

  /**
   * Call {@code startNavigation} passing in a {@link DirectionsRoute} object to begin a navigation session. It is
   * recommend to call {@link MapboxNavigation#getRoute(Position, Position, Callback)} before starting a navigation
   * session.
   *
   * @param route A {@link DirectionsRoute} that makes up the path your user will traverse along.
   * @since 2.0.0
   */
  public void startNavigation(DirectionsRoute route) {
    if (!isServiceAvailable()) {
      Timber.d("MapboxNavigation startNavigation called.");
      this.route = route;
      if (!isBound) {
        context.bindService(getServiceIntent(), connection, 0);
        isBound = true;
      }
      context.startService(getServiceIntent());
    }
  }

  /**
   * Call this method to end a navigation session before the user reaches their destination. There isn't a need to call
   * this once the user reaches their destination. You can use the
   * {@link MapboxNavigation#addAlertLevelChangeListener(AlertLevelChangeListener)} to be notified when the user
   * arrives at their location.
   *
   * @since 2.0.0
   */
  public void endNavigation() {
    if (isServiceAvailable()) {
      Timber.d("MapboxNavigation endNavigation called");
      navigationService.endNavigation();
    }
  }

  /**
   * Optionally, set up navigation notification using the default builder provided in the SDK. Alternatively, you can
   * create your own notifications by using the
   * {@link MapboxNavigation#addAlertLevelChangeListener(AlertLevelChangeListener)} to correctly time your
   * notifications.
   *
   * @param activity The activity being used for the navigation session.
   * @since 2.0.0
   */
  public void setupNotification(Activity activity) {
    if (isServiceAvailable()) {
      Timber.d("MapboxNavigation setting up notification.");
      navigationService.setupNotification(activity);
    }
  }

  /*
   * Directions API request methods
   */

  /**
   * Request navigation to acquire a route and notify your callback when a response comes in. A
   * {@link ServicesException} will be thrown if you haven't set your access token, origin or destination before
   * calling {@code getRoute}.
   *
   * @param callback A callback of type {@link DirectionsResponse} which allows you to handle the Directions API
   *                 response.
   * @since 2.0.0
   */
  public void getRoute(Position origin, Position destination, Callback<DirectionsResponse> callback)
    throws ServicesException {
    this.origin = origin;
    this.destination = destination;
    if (accessToken == null) {
      throw new ServicesException("A Mapbox access token must be passed into your MapboxNavigation instance before"
        + "calling getRoute");
    } else if (origin == null || destination == null) {
      throw new ServicesException("A origin and destination Position must be passed into your MapboxNavigation instance"
        + "before calling getRoute");
    }

    MapboxDirections.Builder directionsBuilder = new MapboxDirections.Builder()
      .setProfile(profile)
      .setAccessToken(accessToken)
      .setOverview(DirectionsCriteria.OVERVIEW_FULL)
      .setOrigin(origin)
      .setDestination(destination)
      .setSteps(true);

    // Optionally set the bearing and radiuses if the developer provider the user bearing. A tolerance of 90 degrees
    // is given.
    if (userBearing != null) {
      directionsBuilder.setBearings(new double[] {getUserOriginBearing(), 90}, new double[] {});
    }
    directionsBuilder.build().enqueueCall(callback);
  }

  public void updateRoute(Position origin, Position destination, final Callback<DirectionsResponse> callback) {
    getRoute(origin, destination, new Callback<DirectionsResponse>() {
      @Override
      public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        if (response.body() == null) {
          Timber.e("No routes found, make sure you set the right user and access token.");
          return;
        } else if (response.body().getRoutes().size() < 1) {
          Timber.e("No routes found");
          return;
        }

        call.enqueue(callback);

        DirectionsRoute route = response.body().getRoutes().get(0);
        MapboxNavigation.this.route = route;

        if (isServiceAvailable()) {
          navigationService.updateRoute(route);
        }
      }

      @Override
      public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
        Timber.e("The request for reroute failed with error: ", throwable);
      }
    });
  }

  /**
   * Get the current origin {@link Position} that will be used for the beginning of the route. If no origin's provided,
   * null will be returned. An origin can be set using the
   * {@link MapboxNavigation#getRoute(Position, Position, Callback)} method.
   *
   * @return A {@link Position} object representing the origin of your route.
   * @since 2.0.0
   */
  public Position getOrigin() {
    return origin;
  }

  /**
   * Get the current destination {@link Position} that will be used for the end of the route. If no destinations's
   * provided, null will be returned. A destination can be set using the
   * {@link MapboxNavigation#getRoute(Position, Position, Callback)} method.
   *
   * @return A {@link Position} object representing the destination of your route.
   * @since 2.0.0
   */
  public Position getDestination() {
    return destination;
  }

  /**
   * Optional but recommended parameter that can be used to route a user in the correct orientation they are already
   * headed in. This will help prevent routing the user to begin the route by performing an illegal U-turn.
   *
   * @param userBearing {@code int} between 0 and 260 representing the users current bearing.
   * @since 2.0.0
   */
  public void setUserOriginBearing(@IntRange(from = 0, to = 360) int userBearing) {
    this.userBearing = userBearing;
  }

  /**
   * Gets the current user bearing that will be used when
   * {@link MapboxNavigation#getRoute(Position, Position, Callback)}'s called. If no value's set, the return value will
   * be {@code null}.
   *
   * @return A value between 0 and 360 or null if the userOriginBearing hasn't been set.
   * @since 2.0.0
   */
  public int getUserOriginBearing() {
    return userBearing;
  }

  /**
   * Calling this passing in true will change the default profile used when requesting your route to not consider
   * traffic when routing. For more accurate timing and ensuring the quickest routes provided to the user, it isn't
   * recommend to disable traffic. It is good if you are looking to reproduce the same route over and over for testing
   * for example.
   *
   * @since 2.0.0
   */
  public void setConsiderTraffic(boolean disableTraffic) {
    if (disableTraffic) {
      profile = DirectionsCriteria.PROFILE_DRIVING;
    } else {
      profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC;
    }
  }

  /*
   * Service methods
   */

  private LocationEngine getLocationEngine() {
    // Use the LostLocationEngine if none is provided
    return locationEngine == null ? new LostLocationEngine(context) : locationEngine;
  }

  private boolean isServiceAvailable() {
    boolean isAvailable = (navigationService != null);
    Timber.d("MapboxNavigation service available: %b", isAvailable);
    return isAvailable;
  }

  private Intent getServiceIntent() {
    return new Intent(context, NavigationService.class);
  }

  private class NavigationServiceConnection implements ServiceConnection {
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
      Timber.d("Connected to service.");
      NavigationService.LocalBinder binder = (NavigationService.LocalBinder) service;
      navigationService = binder.getService();
      navigationService.setLocationEngine(getLocationEngine());
      navigationService.setNavigationEventListeners(navigationEventListeners);

      if (alertLevelChangeListeners != null) {
        navigationService.setAlertLevelChangeListeners(alertLevelChangeListeners);
      }

      if (progressChangeListeners != null) {
        navigationService.setProgressChangeListeners(progressChangeListeners);
      }

      if (offRouteListeners != null) {
        navigationService.setOffRouteListeners(offRouteListeners);
      }

      navigationService.setSnapToRoute(snapToRoute);
      navigationService.startRoute(route);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      Timber.d("Disconnected from service.");
      navigationService = null;
    }
  }
}

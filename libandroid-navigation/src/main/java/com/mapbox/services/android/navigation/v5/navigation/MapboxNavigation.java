package com.mapbox.services.android.navigation.v5.navigation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.exception.NavigationException;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteDetector;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.snap.Snap;
import com.mapbox.services.android.navigation.v5.snap.SnapToRoute;
import com.mapbox.services.android.telemetry.MapboxEvent;
import com.mapbox.services.android.telemetry.MapboxTelemetry;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.location.LostLocationEngine;
import com.mapbox.services.android.telemetry.utils.TelemetryUtils;
import com.mapbox.services.utils.TextUtils;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Callback;
import timber.log.Timber;

/**
 * A MapboxNavigation class for interacting with and customizing a navigation session.
 * <p>
 * Instance of this class are used to setup, customize, start, and end a navigation session.
 *
 * @see <a href="https://www.mapbox.com/android-docs/navigation/">Navigation documentation</a>
 * @since 0.1.0
 */
public class MapboxNavigation implements ServiceConnection, ProgressChangeListener {

  private static final String MAPBOX_NAVIGATION_SDK_IDENTIFIER = "mapbox-navigation-android";
  private static final String MAPBOX_NAVIGATION_UI_SDK_IDENTIFIER = "mapbox-navigation-ui-android";

  private NavigationEventDispatcher navigationEventDispatcher;
  private NavigationService navigationService;
  private DirectionsRoute directionsRoute;
  private MapboxNavigationOptions options;
  private LocationEngine locationEngine;
  private List<Milestone> milestones;
  private SessionState sessionState;
  private final String accessToken;
  private OffRoute offRouteEngine;
  private Snap snapEngine;
  private Context context;
  private boolean isBound;
  private boolean isFromNavigationUi = false;

  /**
   * Constructs a new instance of this class using the default options. This should be used over
   * {@link #MapboxNavigation(Context, String, MapboxNavigationOptions)} if all the default options
   * fit your needs.
   * <p>
   * Initialization will also add the default milestones and create a new LOST location engine
   * which will be used during navigation unless a different engine gets passed in through
   * {@link #setLocationEngine(LocationEngine)}.
   * </p>
   *
   * @param context     required in order to create and bind the navigation service
   * @param accessToken a valid Mapbox access token
   * @since 0.5.0
   */
  public MapboxNavigation(@NonNull Context context, @NonNull String accessToken) {
    this(context, accessToken, MapboxNavigationOptions.builder().build());
  }

  /**
   * Constructs a new instance of this class using a custom built options class. Building a custom
   * {@link MapboxNavigationOptions} object and passing it in allows you to further customize the
   * user experience. While many of the default values have been tested thoroughly, you might find
   * that your app requires special tweaking. Once this class is initialized, the options specified
   * through the options class cannot be modified.
   * <p>
   * Initialization will also add the default milestones and create a new LOST location engine
   * which will be used during navigation unless a different engine gets passed in through
   * {@link #setLocationEngine(LocationEngine)}.
   * </p>
   *
   * @param context     required in order to create and bind the navigation service
   * @param options     a custom built {@code MapboxNavigationOptions} class
   * @param accessToken a valid Mapbox access token
   * @see MapboxNavigationOptions
   * @since 0.5.0
   */
  public MapboxNavigation(@NonNull Context context, @NonNull String accessToken,
                          @NonNull MapboxNavigationOptions options) {
    this.accessToken = accessToken;
    this.context = context;
    this.options = options;
    this.isFromNavigationUi = options.isFromNavigationUi();
    initialize();
  }

  /**
   * In-charge of initializing all variables needed to begin a navigation session. Many values can
   * be changed later on using their corresponding setter. An internal progressChangeListeners used
   * to prevent users from removing it.
   */
  private void initialize() {
    initializeTelemetry();

    // Initialize event dispatcher and add internal listeners
    navigationEventDispatcher = new NavigationEventDispatcher();
    if (!options.manuallyEndNavigationUponCompletion()) {
      navigationEventDispatcher.setInternalProgressChangeListener(this);
    }

    // Create and add default milestones if enabled.
    milestones = new ArrayList<>();
    if (options.defaultMilestonesEnabled()) {
      new DefaultMilestones(this);
    }

    initializeDefaultLocationEngine();

    if (options.snapToRoute()) {
      snapEngine = new SnapToRoute();
    }
    if (options.enableOffRouteDetection()) {
      offRouteEngine = new OffRouteDetector();
    }
  }

  private void initializeTelemetry() {
    validateAccessToken(accessToken);
    String sdkIdentifier = MAPBOX_NAVIGATION_SDK_IDENTIFIER;
    if (isFromNavigationUi) {
      sdkIdentifier = MAPBOX_NAVIGATION_UI_SDK_IDENTIFIER;
    }
    String userAgent = String.format("%s/%s", sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME);
    MapboxTelemetry.getInstance().initialize(context, accessToken, userAgent, sdkIdentifier,
      BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME);
    MapboxTelemetry.getInstance().newUserAgent(userAgent);

    NavigationMetricsWrapper.sdkIdentifier = sdkIdentifier;
    NavigationMetricsWrapper.turnstileEvent();
    // TODO This should be removed when we figure out a solution in Telemetry
    // Force pushing a TYPE_MAP_LOAD event to ensure that the Nav turnstile event is sent
    MapboxTelemetry.getInstance().pushEvent(MapboxEvent.buildMapLoadEvent());
  }

  /**
   * Runtime validation of access token.
   *
   * @throws NavigationException exception thrown when not using a valid accessToken
   */
  private static void validateAccessToken(String accessToken) {
    if (TextUtils.isEmpty(accessToken) || (!accessToken.toLowerCase(Locale.US).startsWith("pk.")
      && !accessToken.toLowerCase(Locale.US).startsWith("sk."))) {
      throw new NavigationException("A valid access token must be passed in when first initializing"
        + " MapboxNavigation");
    }
  }

  /**
   * Since navigation requires location information there should always be a valid location engine
   * which we can use to get information. Therefore, by default we build one.
   */
  private void initializeDefaultLocationEngine() {
    locationEngine = new LostLocationEngine(context);
    locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
    locationEngine.setFastestInterval(1000);
    locationEngine.setInterval(0);
    locationEngine.activate();
  }

  /**
   * When onDestroy gets called, it is safe to remove location updates and deactivate the engine.
   */
  private void disableLocationEngine() {
    if (locationEngine != null) {
      locationEngine.removeLocationEngineListener(null);
      locationEngine.removeLocationUpdates();
      locationEngine.deactivate();
    }
  }

  // Lifecycle

  /**
   * Critical to place inside your navigation activity so that when your application gets destroyed
   * the navigation service unbinds and gets destroyed, preventing any memory leaks. Calling this
   * also removes all listeners that have been attached.
   */
  public void onDestroy() {
    Timber.d("MapboxNavigation onDestroy.");
    endNavigation();
    disableLocationEngine();
    removeNavigationEventListener(null);
    removeProgressChangeListener(null);
    removeMilestoneEventListener(null);
    removeOffRouteListener(null);
  }

  // Public APIs

  /**
   * Navigation {@link Milestone}s provide a powerful way to give your user instructions at custom
   * defined locations along their route. Default milestones are automatically added unless
   * {@link MapboxNavigationOptions#defaultMilestonesEnabled()} is set to false but they can also
   * be individually removed using the {@link #removeMilestone(Milestone)} API. Once a custom
   * milestone is built, it will need to be passed into the navigation SDK through this method.
   * <p>
   * Milestones can only be added once and must be removed and added back if any changes are
   * desired.
   * </p>
   *
   * @param milestone a custom built milestone
   * @since 0.4.0
   */
  public void addMilestone(@NonNull Milestone milestone) {
    if (milestones.contains(milestone)) {
      Timber.w("Milestone has already been added to the stack.");
      return;
    }
    milestones.add(milestone);
  }

  /**
   * Remove a specific milestone by passing in the instance of it. Removal of all the milestones can
   * be achieved by passing in null rather than a specific milestone.
   *
   * @param milestone a milestone you'd like to have removed or null if you'd like to remove all
   *                  milestones
   * @since 0.4.0
   */
  @SuppressWarnings("WeakerAccess") // Public exposed for usage outside SDK
  public void removeMilestone(@Nullable Milestone milestone) {
    if (milestone == null) {
      milestones.clear();
      return;
    } else if (!milestones.contains(milestone)) {
      Timber.w("Milestone attempting to remove does not exist in stack.");
      return;
    }
    milestones.remove(milestone);
  }

  /**
   * Remove a specific milestone by passing in the identifier associated with the milestone you'd
   * like to remove. If the identifier passed in does not match one of the milestones in the list,
   * a warning will return in the log.
   *
   * @param milestoneIdentifier identifier matching one of the milestones
   * @since 0.5.0
   */
  @SuppressWarnings("WeakerAccess") // Public exposed for usage outside SDK
  public void removeMilestone(int milestoneIdentifier) {
    for (Milestone milestone : milestones) {
      if (milestoneIdentifier == milestone.getIdentifier()) {
        removeMilestone(milestone);
        return;
      }
    }
    Timber.w("No milestone found with the specified identifier.");
  }

  /**
   * Navigation needs an instance of location engine in order to acquire user location information
   * and handle events based off of the current information. By default, a LOST location engine is
   * created with the optimal navigation settings. Passing in a custom location engine using this
   * API assumes you have set it to the ideal parameters which are specified below.
   * <p>
   * Although it is not required to set your location engine to these parameters, these values are
   * what we found works best. Note that this also depends on which underlying location service you
   * are using. Reference the corresponding location service documentation for more information and
   * way's you could improve the performance.
   * </p><p>
   * An ideal conditions, the Navigation SDK will receive location updates once every second with
   * mild to high horizontal accuracy. The location update must also contain all information an
   * Android location object would expect including bearing, speed, timestamp, and
   * latitude/longitude.
   * </p><p>
   * Listed below are the ideal conditions for both a LOST location engine and a Google Play
   * Services Location engine.
   * </p><p><ul>
   * <li>Set the location priority to {@code HIGH_ACCURACY}.</li>
   * <li>The fastest interval should be set around 1 second (1000ms). Note that the interval isn't
   * a guaranteed to match this value exactly and is only an estimate.</li>
   * <li>Setting the location engine interval to 0 will result in location updates occurring as
   * quickly as possible within the fastest interval limit placed on it.</li>
   * </ul>
   *
   * @param locationEngine a {@link LocationEngine} used for the navigation session
   * @since 0.1.0
   */
  public void setLocationEngine(@NonNull LocationEngine locationEngine) {
    this.locationEngine = locationEngine;
    // Notify service to get new location engine.
    if (isServiceAvailable()) {
      navigationService.acquireLocationEngine();
    }
  }

  /**
   * Will return the currently set location engine. By default, the LOST location engine that's
   * created on initialization of this class. If a custom location engine is preferred to be used,
   * {@link #setLocationEngine(LocationEngine)} is offered which will replace the default.
   *
   * @return the location engine which is will or currently is being used during the navigation
   * session
   * @since 0.5.0
   */
  @SuppressWarnings("WeakerAccess") // Public exposed for usage outside SDK
  @NonNull
  public LocationEngine getLocationEngine() {
    return locationEngine;
  }

  /**
   * Calling This begins a new navigation session using the provided directions route. this API is
   * also intended to be used when a reroute occurs passing in the updated directions route.
   * <p>
   * On initial start of the navigation session, the navigation services gets created and bound to
   * your activity. Unless disabled, a notification will be displayed to the user and will remain
   * until the service stops running in the background.
   * </p><p>
   * The directions route should be acquired by building a {@link NavigationRoute} object and
   * calling {@link NavigationRoute#getRoute(Callback)} on it. Using navigation route request a
   * route with the required parameters needed while at the same time, allowing for flexibility in
   * other parts of the request.
   * </p>
   *
   * @param directionsRoute a {@link DirectionsRoute} that makes up the path your user should
   *                        traverse along
   * @since 0.1.0
   */
  public void startNavigation(@NonNull DirectionsRoute directionsRoute) {
    this.directionsRoute = directionsRoute;
    Timber.d("MapboxNavigation startNavigation called.");
    if (!isBound) {
      // Navigation sessions initially starting
      sessionState = SessionState.builder()
        .originalDirectionRoute(directionsRoute)
        .currentDirectionRoute(directionsRoute)
        .sessionIdentifier(TelemetryUtils.buildUUID())
        .previousRouteDistancesCompleted(0)
        .startTimestamp(new Date())
        .rerouteCount(0)
        .mockLocation(locationEngine instanceof MockLocationEngine)
        .build();

      Intent intent = getServiceIntent();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent);
      } else {
        context.startService(intent);
      }

      context.bindService(intent, this, Context.BIND_AUTO_CREATE);
      navigationEventDispatcher.onNavigationEvent(true);
    } else {
      // New directionRoute provided
      sessionState = sessionState.toBuilder()
        .currentDirectionRoute(directionsRoute)
        .rerouteCount(sessionState.rerouteCount() + 1)
        .build();

      navigationService.rerouteOccurred();
    }
  }

  /**
   * Call this when the navigation session needs to end before the user reaches their final
   * destination. There isn't a need to manually end the navigation session using this API when the
   * user arrives unless you set {@link MapboxNavigationOptions#manuallyEndNavigationUponCompletion()}
   * to true.
   * <p>
   * Ending the navigation session ends and unbinds the navigation service meaning any milestone,
   * progress change, or off-route listeners will not be invoked anymore. A call returning false
   * will occur to {@link NavigationEventListener#onRunning(boolean)} to notify you when the service
   * ends.
   * </p>
   *
   * @since 0.1.0
   */
  public void endNavigation() {
    Timber.d("MapboxNavigation endNavigation called");
    if (isServiceAvailable()) {
      navigationService.stopSelf();
      context.unbindService(this);
      isBound = false;
      navigationEventDispatcher.onNavigationEvent(false);
    }
  }

  // Listeners

  /**
   * This adds a new milestone event listener which is invoked when a milestone gets triggered. If
   * more then one milestone gets triggered on a location update, each milestone event listener will
   * be invoked for each of those milestones. This is important to consider if you are using voice
   * instructions since this would cause multiple instructions to be said at once. Ideally the
   * milestones setup should avoid triggering too close to each other.
   * <p>
   * It is not possible to add the same listener implementation more then once and a warning will be
   * printed in the log if attempted.
   * </p>
   *
   * @param milestoneEventListener an implementation of {@code MilestoneEventListener} which hasn't
   *                               already been added
   * @see MilestoneEventListener
   * @since 0.4.0
   */
  public void addMilestoneEventListener(@NonNull MilestoneEventListener milestoneEventListener) {
    navigationEventDispatcher.addMilestoneEventListener(milestoneEventListener);
  }

  /**
   * This removes a specific milestone event listener by passing in the instance of it or you can
   * pass in null to remove all the listeners. When {@link #onDestroy()} is called, all listeners
   * get removed automatically, removing the requirement for developers to manually handle this.
   * <p>
   * If the listener you are trying to remove does not exist in the list, a warning will be printed
   * in the log.
   * </p>
   *
   * @param milestoneEventListener an implementation of {@code MilestoneEventListener} which
   *                               currently exist in the milestoneEventListener list
   * @see MilestoneEventListener
   * @since 0.4.0
   */
  @SuppressWarnings("WeakerAccess") // Public exposed for usage outside SDK
  public void removeMilestoneEventListener(@Nullable MilestoneEventListener milestoneEventListener) {
    navigationEventDispatcher.removeMilestoneEventListener(milestoneEventListener);
  }

  /**
   * This adds a new progress change listener which is invoked when a location change occurs and the
   * navigation engine successfully runs it's calculations on it.
   * <p>
   * It is not possible to add the same listener implementation more then once and a warning will be
   * printed in the log if attempted.
   * </p>
   *
   * @param progressChangeListener an implementation of {@code ProgressChangeListener} which hasn't
   *                               already been added
   * @see ProgressChangeListener
   * @since 0.1.0
   */
  public void addProgressChangeListener(@NonNull ProgressChangeListener progressChangeListener) {
    navigationEventDispatcher.addProgressChangeListener(progressChangeListener);
  }

  /**
   * This removes a specific progress change listener by passing in the instance of it or you can
   * pass in null to remove all the listeners. When {@link #onDestroy()} is called, all listeners
   * get removed automatically, removing the requirement for developers to manually handle this.
   * <p>
   * If the listener you are trying to remove does not exist in the list, a warning will be printed
   * in the log.
   * </p>
   *
   * @param progressChangeListener an implementation of {@code ProgressChangeListener} which
   *                               currently exist in the progressChangeListener list
   * @see ProgressChangeListener
   * @since 0.1.0
   */
  public void removeProgressChangeListener(@Nullable ProgressChangeListener progressChangeListener) {
    navigationEventDispatcher.removeProgressChangeListener(progressChangeListener);
  }

  /**
   * This adds a new off route listener which is invoked when the devices location veers off the
   * route and the specified criteria's in {@link MapboxNavigationOptions} have been met.
   * <p>
   * The behavior that causes this listeners callback to get invoked vary depending on whether a
   * custom off route engine has been set using {@link #setOffRouteEngine(OffRoute)}.
   * </p><p>
   * It is not possible to add the same listener implementation more then once and a warning will be
   * printed in the log if attempted.
   * </p>
   *
   * @param offRouteListener an implementation of {@code OffRouteListener} which hasn't already been
   *                         added
   * @see OffRouteListener
   * @since 0.2.0
   */
  public void addOffRouteListener(@NonNull OffRouteListener offRouteListener) {
    navigationEventDispatcher.addOffRouteListener(offRouteListener);
  }

  /**
   * This removes a specific off route listener by passing in the instance of it or you can pass in
   * null to remove all the listeners. When {@link #onDestroy()} is called, all listeners
   * get removed automatically, removing the requirement for developers to manually handle this.
   * <p>
   * If the listener you are trying to remove does not exist in the list, a warning will be printed
   * in the log.
   * </p>
   *
   * @param offRouteListener an implementation of {@code OffRouteListener} which currently exist in
   *                         the offRouteListener list
   * @see OffRouteListener
   * @since 0.2.0
   */
  @SuppressWarnings("WeakerAccess") // Public exposed for usage outside SDK
  public void removeOffRouteListener(@Nullable OffRouteListener offRouteListener) {
    navigationEventDispatcher.removeOffRouteListener(offRouteListener);
  }

  /**
   * This adds a new navigation event listener which is invoked when navigation service begins
   * running in the background and again when the service gets destroyed.
   * <p>
   * It is not possible to add the same listener implementation more then once and a warning will be
   * printed in the log if attempted.
   * </p>
   *
   * @param navigationEventListener an implementation of {@code NavigationEventListener} which
   *                                hasn't already been added
   * @see NavigationEventListener
   * @since 0.1.0
   */
  public void addNavigationEventListener(@NonNull NavigationEventListener navigationEventListener) {
    navigationEventDispatcher.addNavigationEventListener(navigationEventListener);
  }

  /**
   * This removes a specific navigation event listener by passing in the instance of it or you can
   * pass in null to remove all the listeners. When {@link #onDestroy()} is called, all listeners
   * get removed automatically, removing the requirement for developers to manually handle this.
   * <p>
   * If the listener you are trying to remove does not exist in the list, a warning will be printed
   * in the log.
   * </p>
   *
   * @param navigationEventListener an implementation of {@code NavigationEventListener} which
   *                                currently exist in the navigationEventListener list
   * @see NavigationEventListener
   * @since 0.1.0
   */
  public void removeNavigationEventListener(@Nullable NavigationEventListener navigationEventListener) {
    navigationEventDispatcher.removeNavigationEventListener(navigationEventListener);
  }

  // Custom engines

  /**
   * This API is used to pass in a custom implementation of the snapping logic, A default
   * snap-to-route engine is attached when this class is first initialized; setting a custom one
   * will replace it with your own implementation.
   * <p>
   * In general, snap logic can be anything that modifies the device's true location. For more
   * information see the implementation notes in {@link Snap}.
   * </p><p>
   * The engine can be changed at anytime, even during a navigation session.
   * </p>
   *
   * @param snapEngine a custom implementation of the {@code Snap} class
   * @see Snap
   * @since 0.5.0
   */
  @SuppressWarnings("WeakerAccess") // Public exposed for usage outside SDK
  public void setSnapEngine(@NonNull Snap snapEngine) {
    this.snapEngine = snapEngine;
  }

  /**
   * This will return the currently set snap engine which will or is being used during the
   * navigation session. If no snap engine has been set yet, the default engine will be returned.
   *
   * @return the snap engine currently set and will/is being used for the navigation session
   * @see Snap
   * @since 0.5.0
   */
  @SuppressWarnings("WeakerAccess") // Public exposed for usage outside SDK
  @NonNull
  public Snap getSnapEngine() {
    return snapEngine;
  }

  /**
   * This API is used to pass in a custom implementation of the off-route logic, A default
   * off-route detection engine is attached when this class is first initialized; setting a custom
   * one will replace it with your own implementation.
   * <p>
   * The engine can be changed at anytime, even during a navigation session.
   * </p>
   *
   * @param offRouteEngine a custom implementation of the {@code OffRoute} class
   * @see OffRoute
   * @since 0.5.0
   */
  @SuppressWarnings("WeakerAccess") // Public exposed for usage outside SDK
  public void setOffRouteEngine(@NonNull OffRoute offRouteEngine) {
    this.offRouteEngine = offRouteEngine;
  }

  /**
   * This will return the currently set off-route engine which will or is being used during the
   * navigation session. If no off-route engine has been set yet, the default engine will be
   * returned.
   *
   * @return the off-route engine currently set and will/is being used for the navigation session
   * @see OffRoute
   * @since 0.5.0
   */
  @SuppressWarnings("WeakerAccess") // Public exposed for usage outside SDK
  @NonNull
  public OffRoute getOffRouteEngine() {
    return offRouteEngine;
  }

  // Service

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    Timber.v("Arrived event occurred");
    sessionState = sessionState.toBuilder().arrivalTimestamp(new Date()).build();
    NavigationMetricsWrapper.arriveEvent(sessionState, routeProgress, location);
    // Remove all listeners except the onProgressChange by passing in null.
    navigationEventDispatcher.removeOffRouteListener(null);
    // Remove this listener so that the arrival event only occurs once.
    navigationEventDispatcher.removeInternalProgressChangeListener();
  }

  DirectionsRoute getRoute() {
    return directionsRoute;
  }

  List<Milestone> getMilestones() {
    return milestones;
  }

  MapboxNavigationOptions options() {
    return options;
  }

  NavigationEventDispatcher getEventDispatcher() {
    return navigationEventDispatcher;
  }

  SessionState getSessionState() {
    return sessionState;
  }

  void setSessionState(SessionState sessionState) {
    this.sessionState = sessionState;
  }

  private Intent getServiceIntent() {
    return new Intent(context, NavigationService.class);
  }

  private boolean isServiceAvailable() {
    return navigationService != null && isBound;
  }

  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    Timber.d("Connected to service.");
    NavigationService.LocalBinder binder = (NavigationService.LocalBinder) service;
    navigationService = binder.getService();
    navigationService.startNavigation(this);
    isBound = true;
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    Timber.d("Disconnected from service.");
    navigationService = null;
    isBound = false;
  }
}

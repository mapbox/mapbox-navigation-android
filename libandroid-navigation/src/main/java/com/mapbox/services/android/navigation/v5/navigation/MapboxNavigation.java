package com.mapbox.services.android.navigation.v5.navigation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.NavigatorConfig;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.internal.navigation.ElectronicHorizonParams;
import com.mapbox.services.android.navigation.v5.internal.navigation.ElectronicHorizonRequestBuilder;
import com.mapbox.services.android.navigation.v5.internal.navigation.FreeDriveLocationUpdater;
import com.mapbox.services.android.navigation.v5.internal.navigation.MapboxNavigator;
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationEngineFactory;
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationEventDispatcher;
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationService;
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationTelemetry;
import com.mapbox.services.android.navigation.v5.internal.navigation.RouteRefresher;
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.FeedbackEvent;
import com.mapbox.services.android.navigation.v5.navigation.metrics.MapboxMetricsReporter;
import com.mapbox.services.android.navigation.v5.internal.utils.ValidationUtils;
import com.mapbox.services.android.navigation.v5.location.RawLocationListener;
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone;
import com.mapbox.services.android.navigation.v5.navigation.camera.Camera;
import com.mapbox.services.android.navigation.v5.navigation.camera.SimpleCamera;
import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.snap.Snap;
import timber.log.Timber;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.BANNER_INSTRUCTION_MILESTONE_ID;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NON_NULL_APPLICATION_CONTEXT_REQUIRED;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.VOICE_INSTRUCTION_MILESTONE_ID;

/**
 * A MapboxNavigation class for interacting with and customizing a navigation session.
 * <p>
 * Instance of this class are used to setup, customize, start, and end a navigation session.
 *
 * @see <a href="https://www.mapbox.com/android-docs/navigation/">Navigation documentation</a>
 * @since 0.1.0
 */
public class MapboxNavigation implements ServiceConnection {

  private static final String MAPBOX_NAVIGATION_USER_AGENT_BASE = "mapbox-navigation-android";
  private static final String MAPBOX_NAVIGATION_UI_USER_AGENT_BASE = "mapbox-navigation-ui-android";
  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 500;
  private NavigationEventDispatcher navigationEventDispatcher;
  private NavigationEngineFactory navigationEngineFactory;
  private NavigationTelemetry navigationTelemetry = null;
  private NavigationService navigationService;
  private MapboxNavigator mapboxNavigator;
  private DirectionsRoute directionsRoute;
  private MapboxNavigationOptions options;
  private LocationEngine locationEngine;
  private FreeDriveLocationUpdater freeDriveLocationUpdater;
  private LocationEngineRequest locationEngineRequest;
  private Set<Milestone> milestones;
  private final String accessToken;
  private Context applicationContext;
  private boolean isBound;
  private RouteRefresher routeRefresher;
  private AtomicBoolean isFreeDriveEnabled = new AtomicBoolean(false);
  private AtomicBoolean isFreeDriveConfigured = new AtomicBoolean(false);
  private AtomicBoolean isActiveGuidanceOnGoing = new AtomicBoolean(false);

  static {
    NavigationLibraryLoader.Companion.load();
  }

  /**
   * Constructs a new instance of this class using the default options. This should be used over
   * {@link #MapboxNavigation(Context, String, MapboxNavigationOptions)} if all the default options
   * fit your needs.
   * <p>
   * Initialization will also add the default milestones and create a new location engine
   * which will be used during navigation unless a different engine gets passed in through
   * {@link #setLocationEngine(LocationEngine)}.
   * </p>
   *
   * @param context     required in order to create and bind the navigation service
   * @param accessToken a valid Mapbox access token
   * @since 0.5.0
   */
  public MapboxNavigation(@NonNull Context context, @NonNull String accessToken) {
    this(context, accessToken, new MapboxNavigationOptions.Builder().build());
  }

  /**
   * Constructs a new instance of this class using a custom built options class. Building a custom
   * {@link MapboxNavigationOptions} object and passing it in allows you to further customize the
   * user experience. While many of the default values have been tested thoroughly, you might find
   * that your app requires special tweaking. Once this class is initialized, the options specified
   * through the options class cannot be modified.
   * <p>
   * Initialization will also add the default milestones and create a new location engine
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
    initializeContext(context);
    this.accessToken = accessToken;
    this.options = options;
    initialize();
  }

  /**
   * Constructs a new instance of this class using a custom built options class. Building a custom
   * {@link MapboxNavigationOptions} object and passing it in allows you to further customize the
   * user experience. Once this class is initialized, the options specified
   * through the options class cannot be modified.
   *
   * @param context        required in order to create and bind the navigation service
   * @param accessToken    a valid Mapbox access token
   * @param options        a custom built {@code MapboxNavigationOptions} class
   * @param locationEngine a LocationEngine to provide Location updates
   * @see MapboxNavigationOptions
   * @since 0.19.0
   */
  public MapboxNavigation(@NonNull Context context, @NonNull String accessToken,
                          @NonNull MapboxNavigationOptions options, @NonNull LocationEngine locationEngine) {
    initializeContext(context);
    this.accessToken = accessToken;
    this.options = options;
    this.locationEngine = locationEngine;
    initialize();
  }


  // TODO public?
  // Package private (no modifier) for testing purposes
  public MapboxNavigation(@NonNull Context context, @NonNull String accessToken,
                          NavigationTelemetry navigationTelemetry, LocationEngine locationEngine,
                          MapboxNavigator mapboxNavigator) {
    initializeContext(context);
    this.accessToken = accessToken;
    this.options = new MapboxNavigationOptions.Builder().build();
    this.navigationTelemetry = navigationTelemetry;
    this.locationEngine = locationEngine;
    this.mapboxNavigator = mapboxNavigator;
    initializeForTest(context);
  }

  // Package private (no modifier) for testing purposes
  MapboxNavigation(@NonNull Context context, @NonNull String accessToken,
                   @NonNull MapboxNavigationOptions options, NavigationTelemetry navigationTelemetry,
                   LocationEngine locationEngine, Navigator navigator,
                   FreeDriveLocationUpdater freeDriveLocationUpdater) {
    initializeContext(context);
    this.accessToken = accessToken;
    this.options = options;
    this.navigationTelemetry = navigationTelemetry;
    this.locationEngine = locationEngine;
    this.mapboxNavigator = new MapboxNavigator(navigator);
    initializeForTest(context);
    this.freeDriveLocationUpdater = freeDriveLocationUpdater;
  }

  // Lifecycle

  /**
   * Critical to place inside your navigation activity so that when your application gets destroyed
   * the navigation service unbinds and gets destroyed, preventing any memory leaks. Calling this
   * also removes all listeners that have been attached.
   */
  public void onDestroy() {
    killNavigation();
    removeOffRouteListener(null);
    removeProgressChangeListener(null);
    removeMilestoneEventListener(null);
    removeNavigationEventListener(null);
    removeFasterRouteListener(null);
    removeRawLocationListener(null);
    removeEnhancedLocationListener(null);
  }

  // Public APIs

  /**
   * Navigation {@link Milestone}s provide a powerful way to give your user instructions at custom
   * defined locations along their route. Default milestones are automatically added unless
   * {@link MapboxNavigationOptions#getDefaultMilestonesEnabled()} is set to false but they can also
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
    boolean milestoneAdded = milestones.add(milestone);
    if (!milestoneAdded) {
      Timber.w("Milestone has already been added to the stack.");
    }
  }

  /**
   * Adds the given list of {@link Milestone} to be triggered during navigation.
   * <p>
   * Milestones can only be added once and must be removed and added back if any changes are
   * desired.
   * </p>
   *
   * @param milestones a list of custom built milestone
   * @since 0.14.0
   */
  public void addMilestones(@NonNull List<Milestone> milestones) {
    boolean milestonesAdded = this.milestones.addAll(milestones);
    if (!milestonesAdded) {
      Timber.w("These milestones have already been added to the stack.");
    }
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
   * and handle events based off of the current information. By default, a {@link LocationEngine} is
   * created using {@link LocationEngineProvider#getBestLocationEngine(Context)}.
   * <p>
   * In ideal conditions, the Navigation SDK will receive location updates once every second with
   * mild to high horizontal accuracy. The location update must also contain all information an
   * Android location object would expect including bearing, speed, timestamp, and
   * latitude/longitude.
   * </p>
   * <p>
   * This method can be called during an active navigation session.  The active {@link LocationEngine} will be
   * replaced and the new one (passed via this method) will be activated with the current {@link LocationEngineRequest}.
   * </p>
   *
   * @param locationEngine a {@link LocationEngine} used for the navigation session
   * @since 0.1.0
   */
  public void setLocationEngine(@NonNull LocationEngine locationEngine) {
    this.locationEngine = locationEngine;
    freeDriveLocationUpdater.updateLocationEngine(locationEngine);
    // Setup telemetry with new engine
    navigationTelemetry.updateLocationEngineNameAndSimulation(locationEngine);
    // Notify service to get new location engine.
    if (isServiceAvailable()) {
      navigationService.updateLocationEngine(locationEngine);
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
   * This method updates the {@link LocationEngineRequest} that is used with the {@link LocationEngine}.
   * <p>
   * If a request is not provided via {@link MapboxNavigation#setLocationEngineRequest(LocationEngineRequest)},
   * a default will be provided with optimized settings for navigation.
   * </p>
   * <p>
   * This method can be called during an active navigation session.  The active {@link LocationEngineRequest} will be
   * replaced and the new one (passed via this method) will be activated with the current {@link LocationEngine}.
   * </p>
   *
   * @param locationEngineRequest to be used with the current {@link LocationEngine}
   */
  public void setLocationEngineRequest(@NonNull LocationEngineRequest locationEngineRequest) {
    this.locationEngineRequest = locationEngineRequest;
    freeDriveLocationUpdater.updateLocationEngineRequest(locationEngineRequest);
    if (isServiceAvailable()) {
      navigationService.updateLocationEngineRequest(locationEngineRequest);
    }
  }

  /**
   * Calling this method begins a new navigation session using the provided directions route. This API is
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
    startNavigationWith(directionsRoute, DirectionsRouteType.NEW_ROUTE);
  }

  /**
   * Calling this method with {@link DirectionsRouteType#NEW_ROUTE} begins a new navigation session using the
   * provided directions route.  If called with {@link DirectionsRouteType#FRESH_ROUTE}, only leg annotation data
   * will be update - can be used with {@link RouteRefresh}.
   *
   * @param directionsRoute a {@link DirectionsRoute} that makes up the path your user should
   *                        traverse along
   * @param routeType       either new or fresh to determine what data navigation should consider
   * @see MapboxNavigation#startNavigation(DirectionsRoute)
   */
  public void startNavigation(@NonNull DirectionsRoute directionsRoute, @NonNull DirectionsRouteType routeType) {
    startNavigationWith(directionsRoute, routeType);
  }

  /**
   * Call this when the navigation session needs to end before the user reaches their final
   * destination.
   * <p>
   * Ending the navigation session ends and unbinds the navigation service meaning any milestone,
   * progress change, or off-route listeners will not be invoked anymore. A call returning false
   * will occur to {@link NavigationEventListener#onRunning(boolean)} to notify you when the service
   * ends.
   * </p>
   *
   * @since 0.1.0
   */
  public void stopNavigation() {
    isActiveGuidanceOnGoing.set(false);
    if (isFreeDriveEnabled.get()) {
      enableFreeDrive();
    }
    stopNavigationService();
  }

  private void killNavigation() {
    killFreeDrive();
    stopNavigationService();
  }

  private void killFreeDrive() {
    if (isFreeDriveConfigured.get()) {
      freeDriveLocationUpdater.kill();
    }
  }

  private void stopNavigationService() {
    Timber.d("MapboxNavigation stopped");
    if (isServiceAvailable()) {
      navigationTelemetry.stopSession();
      applicationContext.unbindService(this);
      isBound = false;
      navigationService.endNavigation();
      MapboxMetricsReporter.disable();
      navigationService.stopSelf();
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

  /**
   * This adds a new faster route listener which is invoked when a new, faster {@link DirectionsRoute}
   * has been retrieved by the specified criteria in {@link FasterRoute}.
   * <p>
   * The behavior that causes this listeners callback to get invoked vary depending on whether a
   * custom faster route engine has been set using {@link #setFasterRouteEngine(FasterRoute)}.
   * </p><p>
   * It is not possible to add the same listener implementation more then once and a warning will be
   * printed in the log if attempted.
   * </p>
   *
   * @param fasterRouteListener an implementation of {@code FasterRouteListener}
   * @see FasterRouteListener
   * @since 0.9.0
   */
  public void addFasterRouteListener(@NonNull FasterRouteListener fasterRouteListener) {
    navigationEventDispatcher.addFasterRouteListener(fasterRouteListener);
  }

  /**
   * This removes a specific faster route listener by passing in the instance of it or you can pass in
   * null to remove all the listeners. When {@link #onDestroy()} is called, all listeners
   * get removed automatically, removing the requirement for developers to manually handle this.
   * <p>
   * If the listener you are trying to remove does not exist in the list, a warning will be printed
   * in the log.
   * </p>
   *
   * @param fasterRouteListener an implementation of {@code FasterRouteListener} which currently exist in
   *                            the fasterRouteListeners list
   * @see FasterRouteListener
   * @since 0.9.0
   */
  @SuppressWarnings("WeakerAccess") // Public exposed for usage outside SDK
  public void removeFasterRouteListener(@Nullable FasterRouteListener fasterRouteListener) {
    navigationEventDispatcher.removeFasterRouteListener(fasterRouteListener);
  }

  /**
   * This adds a new raw location listener which is invoked when a new {@link android.location.Location}
   * has been pushed by the {@link LocationEngine}.
   * <p>
   * It is not possible to add the same listener implementation more then once and a warning will be
   * printed in the log if attempted.
   *
   * @param rawLocationListener an implementation of {@code RawLocationListener}
   */
  public void addRawLocationListener(@NonNull RawLocationListener rawLocationListener) {
    navigationEventDispatcher.addRawLocationListener(rawLocationListener);
  }

  /**
   * This removes a specific raw location listener by passing in the instance of it or you can pass in
   * null to remove all the listeners. When {@link #onDestroy()} is called, all listeners
   * get removed automatically, removing the requirement for developers to manually handle this.
   * <p>
   * If the listener you are trying to remove does not exist in the list, a warning will be printed
   * in the log.
   *
   * @param rawLocationListener an implementation of {@code RawLocationListener}
   */
  public void removeRawLocationListener(@Nullable RawLocationListener rawLocationListener) {
    navigationEventDispatcher.removeRawLocationListener(rawLocationListener);
  }

  /**
   * This adds a new enhanced location listener which is invoked when the best enhanced
   * {@link android.location.Location} has been pushed. Either snapped (active guidance),
   * map matched (free drive) or raw.
   * <p>
   * The behavior that causes this listeners callback to get invoked vary depending on whether
   * free drive has been enabled using {@link #enableFreeDrive()} or disabled using
   * {@link #disableFreeDrive()}.
   * </p><p>
   * It is not possible to add the same listener implementation more then once and a warning will be
   * printed in the log if attempted.
   * </p>
   *
   * @param enhancedLocationListener an implementation of {@code EnhancedLocationListener}
   */
  public void addEnhancedLocationListener(@NonNull EnhancedLocationListener enhancedLocationListener) {
    navigationEventDispatcher.addEnhancedLocationListener(enhancedLocationListener);
  }

  /**
   * This removes a specific enhanced location listener by passing in the instance of it or you can
   * pass in null to remove all the listeners. When {@link #onDestroy()} is called, all listeners
   * get removed automatically, removing the requirement for developers to manually handle this.
   * <p>
   * If the listener you are trying to remove does not exist in the list, a warning will be printed
   * in the log.
   *
   * @param enhancedLocationListener an implementation of {@code EnhancedLocationListener}
   */
  public void removeEnhancedLocationListener(@Nullable EnhancedLocationListener enhancedLocationListener) {
    navigationEventDispatcher.removeEnhancedLocationListener(enhancedLocationListener);
  }

  /**
   * Calling this method enables free drive mode.
   * <p>
   * Best enhanced {@link Location} updates are received if an {@link EnhancedLocationListener} has been
   * added using {@link #addEnhancedLocationListener(EnhancedLocationListener)}.
   */
  public void enableFreeDrive() {
    isFreeDriveEnabled.set(true);
    if (!isFreeDriveConfigured.get()) {
      String tilePath = new File(applicationContext.getFilesDir(), "2019_04_13-00_00_11")
          .getAbsolutePath();
      freeDriveLocationUpdater.configure(tilePath, new OnOfflineTilesConfiguredCallback() {
        @Override
        public void onConfigured(int numberOfTiles) {
          Timber.d("DEBUG: onConfigured %d", numberOfTiles);
          isFreeDriveConfigured.set(true);
          if (!isActiveGuidanceOnGoing.get() && isFreeDriveEnabled.get()) {
            freeDriveLocationUpdater.start();
          }
        }

        @Override
        public void onConfigurationError(@NotNull OfflineError error) {
          Timber.e("Free drive: onConfigurationError %s", error.getMessage());
          isFreeDriveConfigured.set(false);
        }
      });
    } else {
      if (!isActiveGuidanceOnGoing.get()) {
        freeDriveLocationUpdater.start();
      }
    }
  }

  /**
   * Calling this method disables free drive mode.
   */
  public void disableFreeDrive() {
    isFreeDriveEnabled.set(false);
    if (isFreeDriveConfigured.get()) {
      freeDriveLocationUpdater.stop();
    }
  }

  // Custom engines

  /**
   * Navigation uses a camera engine to determine the camera position while routing.
   * By default, it uses a {@link SimpleCamera}. If you would like to customize how the camera is
   * positioned, create a new {@link Camera} and set it here.
   *
   * @param cameraEngine camera engine used to configure camera position while routing
   * @since 0.10.0
   */
  public void setCameraEngine(@NonNull Camera cameraEngine) {
    navigationEngineFactory.updateCameraEngine(cameraEngine);
  }

  /**
   * Returns the current camera engine used to configure the camera position while routing. By default,
   * a {@link SimpleCamera} is used.
   *
   * @return camera engine used to configure camera position while routing
   * @since 0.10.0
   */
  @NonNull
  public Camera getCameraEngine() {
    return navigationEngineFactory.retrieveCameraEngine();
  }

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
    navigationEngineFactory.updateSnapEngine(snapEngine);
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
  public Snap getSnapEngine() {
    return navigationEngineFactory.retrieveSnapEngine();
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
    navigationEngineFactory.updateOffRouteEngine(offRouteEngine);
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
    return navigationEngineFactory.retrieveOffRouteEngine();
  }

  /**
   * This API is used to pass in a custom implementation of the faster-route detection logic, A default
   * faster-route detection engine is attached when this class is first initialized; setting a custom
   * one will replace it with your own implementation.
   * <p>
   * The engine can be changed at anytime, even during a navigation session.
   * </p>
   *
   * @param fasterRouteEngine a custom implementation of the {@link FasterRoute} class
   * @see FasterRoute
   * @since 0.9.0
   */
  @SuppressWarnings("WeakerAccess") // Public exposed for usage outside SDK
  public void setFasterRouteEngine(@NonNull FasterRoute fasterRouteEngine) {
    navigationEngineFactory.updateFasterRouteEngine(fasterRouteEngine);
  }

  /**
   * This will return the currently set faster-route engine which will or is being used during the
   * navigation session. If no faster-route engine has been set yet, the default engine will be
   * returned.
   *
   * @return the faster-route engine currently set and will/is being used for the navigation session
   * @see FasterRoute
   * @since 0.9.0
   */
  @SuppressWarnings("WeakerAccess") // Public exposed for usage outside SDK
  @NonNull
  public FasterRoute getFasterRouteEngine() {
    return navigationEngineFactory.retrieveFasterRouteEngine();
  }

  /**
   * Creates a new {@link FeedbackEvent} with a given type, description, and source.
   * <p>
   * Returns a {@link String} feedbackId that can be used to update or cancel this feedback event.
   * There is a 20 second time period set after this method is called to do so.
   *
   * @param feedbackType from list of set feedback types
   * @param description  an option description to provide more detail about the feedback
   * @param source       either from the drop-in UI or a reroute
   * @return String feedbackId
   * @since 0.7.0
   */
  public String recordFeedback(@FeedbackEvent.FeedbackType String feedbackType,
                               String description, @FeedbackEvent.FeedbackSource String source) {
    return navigationTelemetry.recordFeedbackEvent(feedbackType, description, source);
  }

  /**
   * Updates an existing feedback event generated by {@link MapboxNavigation#recordFeedback(String, String, String)}.
   * <p>
   * Uses a feedback ID to find the correct event and then adjusts the feedbackType and description.
   *
   * @param feedbackId   generated from {@link MapboxNavigation#recordFeedback(String, String, String)}
   * @param feedbackType from list of set feedback types
   * @param description  an optional description to provide more detail about the feedback
   * @param screenshot   an optional encoded screenshot to provide more detail about the feedback
   * @since 0.8.0
   */
  public void updateFeedback(String feedbackId, @FeedbackEvent.FeedbackType String feedbackType,
                             String description, String screenshot) {
    navigationTelemetry.updateFeedbackEvent(feedbackId, feedbackType, description, screenshot);
  }

  /**
   * Cancels an existing feedback event generated by {@link MapboxNavigation#recordFeedback(String, String, String)}.
   * <p>
   * Uses a feedback ID to find the correct event and then cancels it (will no longer be recorded).
   *
   * @param feedbackId generated from {@link MapboxNavigation#recordFeedback(String, String, String)}
   * @since 0.7.0
   */
  public void cancelFeedback(String feedbackId) {
    navigationTelemetry.cancelFeedback(feedbackId);
  }

  /**
   * Use this method to update the leg index of the current {@link DirectionsRoute}
   * being traveled along.
   * <p>
   * An index passed here that is not valid will be ignored.  Please note, the leg index
   * will automatically increment by default.  To disable this,
   * use {@link MapboxNavigationOptions#enableAutoIncrementLegIndex()}.
   *
   * @param legIndex to be set
   * @return true if leg index updated, false otherwise
   */
  public boolean updateRouteLegIndex(int legIndex) {
    if (checkInvalidLegIndex(legIndex)) {
      return false;
    }
//    mapboxNavigator.updateLegIndex(legIndex);
    return true;
  }

  public String retrieveHistory() {
    return mapboxNavigator.retrieveHistory();
  }

  public void toggleHistory(boolean isEnabled) {
    mapboxNavigator.toggleHistory(isEnabled);
  }

  public void addHistoryEvent(String eventType, String eventJsonProperties) {
    mapboxNavigator.addHistoryEvent(eventType, eventJsonProperties);
  }

  public String retrieveSsmlAnnouncementInstruction(int index) {
//    return mapboxNavigator.retrieveVoiceInstruction(index).getSsmlAnnouncement();
    return "abc";
  }

  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    Timber.d("Connected to service.");
    NavigationService.LocalBinder binder = (NavigationService.LocalBinder) service;
    if (binder != null) {
      navigationService = binder.getService();
      navigationService.startNavigation(this);
      isBound = true;
    }
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    Timber.d("Disconnected from service.");
    navigationService = null;
    isBound = false;
  }

  // TODO public?
  public String obtainAccessToken() {
    return accessToken;
  }

  // TODO public?
  public DirectionsRoute getRoute() {
    return directionsRoute;
  }

  // TODO public?
  public List<Milestone> getMilestones() {
    return new ArrayList<>(milestones);
  }

  // TODO public?
  public MapboxNavigationOptions options() {
    return options;
  }

  // TODO public?
  public NavigationEventDispatcher getEventDispatcher() {
    return navigationEventDispatcher;
  }

  // TODO public?
  public NavigationEngineFactory retrieveEngineFactory() {
    return navigationEngineFactory;
  }

  // TODO public?
  public MapboxNavigator retrieveMapboxNavigator() {
    return mapboxNavigator;
  }

  // TODO public?
  @NonNull
  public LocationEngineRequest retrieveLocationEngineRequest() {
    return locationEngineRequest;
  }

  // TODO public?
  @Nullable
  public RouteRefresher retrieveRouteRefresher() {
    return routeRefresher;
  }

  private void initializeForTest(Context context) {
    // Initialize event dispatcher and add internal listeners
    navigationEventDispatcher = new NavigationEventDispatcher();
    navigationEngineFactory = new NavigationEngineFactory();
    locationEngine = obtainLocationEngine();
    locationEngineRequest = obtainLocationEngineRequest();
    OfflineNavigator offlineNavigator = new OfflineNavigator(mapboxNavigator.getNavigator(),
            "2019_04_13-00_00_11", "https://api-routing-tiles-staging.tilestream.net",
            accessToken);

    freeDriveLocationUpdater = new FreeDriveLocationUpdater(
        locationEngine,
        locationEngineRequest,
        navigationEventDispatcher,
        mapboxNavigator,
        offlineNavigator,
        Executors.newScheduledThreadPool(2),
        ElectronicHorizonRequestBuilder.INSTANCE,
        new ElectronicHorizonParams.Builder().build());

    initializeTelemetry(context);

    // Create and add default milestones if enabled.
    milestones = new HashSet<>();
    if (options.getDefaultMilestonesEnabled()) {
      addMilestone(new VoiceInstructionMilestone.Builder().setIdentifier(VOICE_INSTRUCTION_MILESTONE_ID).build());
      addMilestone(new BannerInstructionMilestone.Builder().setIdentifier(BANNER_INSTRUCTION_MILESTONE_ID).build());
    }
  }

  /**
   * In-charge of initializing all variables needed to begin a navigation session. Many values can
   * be changed later on using their corresponding setter. An internal progressChangeListeners used
   * to prevent users from removing it.
   */
  private void initialize() {
    mapboxNavigator = new MapboxNavigator(configureNavigator());
    // Initialize event dispatcher and add internal listeners
    navigationEventDispatcher = new NavigationEventDispatcher();
    navigationEventDispatcher.addProgressChangeListener(new ProgressChangeListener() {
      @Override
      public void onProgressChange(@NotNull Location location, @NotNull RouteProgress routeProgress) {
        navigationEventDispatcher.onEnhancedLocationUpdate(location);
      }
    });
    navigationEngineFactory = new NavigationEngineFactory();
    locationEngine = obtainLocationEngine();
    locationEngineRequest = obtainLocationEngineRequest();
    OfflineNavigator offlineNavigator = new OfflineNavigator(mapboxNavigator.getNavigator(),
            "2019_04_13-00_00_11", "https://api-routing-tiles-staging.tilestream.net",
            accessToken); // TODO Replace with an api-routing-tiles-staging valid one

    freeDriveLocationUpdater = new FreeDriveLocationUpdater(
        locationEngine,
        locationEngineRequest,
        navigationEventDispatcher,
        mapboxNavigator,
        offlineNavigator,
        Executors.newScheduledThreadPool(2),
        ElectronicHorizonRequestBuilder.INSTANCE,
        new ElectronicHorizonParams.Builder().build());
    initializeTelemetry(applicationContext);

    // Create and add default milestones if enabled.
    milestones = new HashSet<>();
    if (options.getDefaultMilestonesEnabled()) {
      addMilestone(new VoiceInstructionMilestone.Builder().setIdentifier(VOICE_INSTRUCTION_MILESTONE_ID).build());
      addMilestone(new BannerInstructionMilestone.Builder().setIdentifier(BANNER_INSTRUCTION_MILESTONE_ID).build());
    }
  }

  private void initializeContext(Context context) {
    if (context == null || context.getApplicationContext() == null) {
      throw new IllegalArgumentException(NON_NULL_APPLICATION_CONTEXT_REQUIRED);
    }
    applicationContext = context.getApplicationContext();
  }

  @NotNull
  private Navigator configureNavigator() {
    Navigator navigator = new Navigator(null, null, null);
    NavigatorConfig navigatorConfig = navigator.getConfig();
    navigatorConfig.setOffRouteThreshold(options.getOffRouteThreshold());
    navigatorConfig.setOffRouteThresholdWhenNearIntersection(options.getOffRouteThresholdWhenNearIntersection());
    navigatorConfig.setIntersectionRadiusForOffRouteDetection(options.getIntersectionRadiusForOffRouteDetection());
//    navigator.setConfig(navigatorConfig);
    return navigator;
  }

  private void initializeTelemetry(Context context) {
    navigationTelemetry = obtainTelemetry();
    MapboxMetricsReporter.init(
            context,
            accessToken,
            obtainUserAgent(options)
    );
    MapboxMetricsReporter.toggleLogging(options.isDebugLoggingEnabled());
    navigationTelemetry.initialize(
        applicationContext,
        accessToken,
        this,
        MapboxMetricsReporter.INSTANCE
    );
  }

  private NavigationTelemetry obtainTelemetry() {
    if (navigationTelemetry == null) {
      return NavigationTelemetry.INSTANCE;
    }
    return navigationTelemetry;
  }

  @NonNull
  private LocationEngine obtainLocationEngine() {
    if (locationEngine == null) {
      return LocationEngineProvider.getBestLocationEngine(applicationContext);
    }

    return locationEngine;
  }

  @NonNull
  private LocationEngineRequest obtainLocationEngineRequest() {
    if (locationEngineRequest == null) {
      return new LocationEngineRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
          .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
          .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
          .build();
    }

    return locationEngineRequest;
  }

  private void startNavigationWith(@NonNull DirectionsRoute directionsRoute, DirectionsRouteType routeType) {
    ValidationUtils.validDirectionsRoute(directionsRoute, options.getDefaultMilestonesEnabled());
    this.directionsRoute = directionsRoute;
    routeRefresher = new RouteRefresher(this, new RouteRefresh(accessToken, applicationContext));
    mapboxNavigator.updateRoute(directionsRoute, routeType);
    isActiveGuidanceOnGoing.set(true);
    if (!isBound) {
      disableFreeDrive();
      navigationTelemetry.startSession(directionsRoute, locationEngine);
      startNavigationService();
      navigationEventDispatcher.onNavigationEvent(true);
    } else {
      navigationTelemetry.updateSessionRoute(directionsRoute);
    }
  }

  private void startNavigationService() {
    Intent intent = getServiceIntent();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      applicationContext.startForegroundService(intent);
    } else {
      applicationContext.startService(intent);
    }
    applicationContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
  }

  private Intent getServiceIntent() {
    return new Intent(applicationContext, NavigationService.class);
  }

  private boolean isServiceAvailable() {
    return navigationService != null && isBound;
  }

  private boolean checkInvalidLegIndex(int legIndex) {
    int legSize = directionsRoute.legs().size();
    if (legIndex < 0 || legIndex > legSize - 1) {
      Timber.e("Invalid leg index update: %s Current leg index size: %s", legIndex, legSize);
      return true;
    }
    return false;
  }

  private static String obtainUserAgent(MapboxNavigationOptions options) {
    if (options.isFromNavigationUi()) {
      return MAPBOX_NAVIGATION_UI_USER_AGENT_BASE + BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME;
    } else {
      return MAPBOX_NAVIGATION_USER_AGENT_BASE + BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME;
    }
  }
}

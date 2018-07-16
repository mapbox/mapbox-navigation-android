package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.telemetry.Event;
import com.mapbox.android.telemetry.TelemetryUtils;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.core.utils.TextUtils;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.exception.NavigationException;
import com.mapbox.services.android.navigation.v5.location.MetricsLocation;
import com.mapbox.services.android.navigation.v5.navigation.metrics.FeedbackEvent;
import com.mapbox.services.android.navigation.v5.navigation.metrics.NavigationMetricListener;
import com.mapbox.services.android.navigation.v5.navigation.metrics.RerouteEvent;
import com.mapbox.services.android.navigation.v5.navigation.metrics.SessionState;
import com.mapbox.services.android.navigation.v5.navigation.metrics.TelemetryEvent;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

class NavigationTelemetry implements LocationEngineListener, NavigationMetricListener {

  private static NavigationTelemetry instance;
  private boolean isInitialized = false;

  private static final String MAPBOX_NAVIGATION_SDK_IDENTIFIER = "mapbox-navigation-android";
  private static final String MAPBOX_NAVIGATION_UI_SDK_IDENTIFIER = "mapbox-navigation-ui-android";
  private static final String MOCK_PROVIDER = "com.mapbox.services.android.navigation.v5.location.MockLocationEngine";
  private static final int TWENTY_SECOND_INTERVAL = 20;

  private List<RerouteEvent> queuedRerouteEvents = new ArrayList<>();
  private List<FeedbackEvent> queuedFeedbackEvents = new ArrayList<>();

  private MetricsRouteProgress metricProgress;
  private MetricsLocation metricLocation;

  private NavigationEventDispatcher eventDispatcher;
  private NavigationLifecycleMonitor lifecycleMonitor;
  private LocationEngine navigationLocationEngine;
  private SessionState navigationSessionState;
  private RingBuffer<Location> locationBuffer;
  private Date lastRerouteDate;

  private boolean isOffRoute;
  private boolean isConfigurationChange;

  private NavigationTelemetry() {
    locationBuffer = new RingBuffer<>(40);
    metricLocation = new MetricsLocation(null);
    metricProgress = new MetricsRouteProgress(null);
    navigationSessionState = SessionState.builder().build();
  }

  /**
   * Primary access method (using singleton pattern)
   *
   * @return NavigationTelemetry
   */
  public static synchronized NavigationTelemetry getInstance() {
    if (instance == null) {
      instance = new NavigationTelemetry();
    }

    return instance;
  }

  @Override
  public void onConnected() {
    // No-op
  }

  @Override
  public void onLocationChanged(Location location) {
    updateCurrentLocation(location);
  }

  @Override
  public void onRouteProgressUpdate(RouteProgress routeProgress) {
    this.metricProgress = new MetricsRouteProgress(routeProgress);

    boolean isValidDeparture = navigationSessionState.startTimestamp() == null
      && routeProgress.currentLegProgress().distanceTraveled() > 0;
    if (isValidDeparture) {
      navigationSessionState = navigationSessionState.toBuilder()
        .startTimestamp(new Date())
        .build();
      updateLifecyclePercentages();
      NavigationMetricsWrapper.departEvent(navigationSessionState, metricProgress, metricLocation.getLocation());
    }
  }

  @Override
  public void onOffRouteEvent(Location offRouteLocation) {
    if (!isOffRoute) {
      updateDistanceCompleted();
      queueRerouteEvent();
      isOffRoute = true;
    }
  }

  @Override
  public void onArrival(RouteProgress routeProgress) {
    // Update arrival time stamp
    navigationSessionState = navigationSessionState.toBuilder().arrivalTimestamp(new Date()).build();
    updateLifecyclePercentages();
    // Send arrival event
    NavigationMetricsWrapper.arriveEvent(navigationSessionState, routeProgress, metricLocation.getLocation());
  }

  void initialize(@NonNull Context context, @NonNull String accessToken,
                  MapboxNavigation navigation, LocationEngine locationEngine) {
    if (!isInitialized) {
      // Setup the location engine
      updateLocationEngine(locationEngine);

      validateAccessToken(accessToken);
      NavigationMetricsWrapper.init(context, accessToken, BuildConfig.MAPBOX_NAVIGATION_EVENTS_USER_AGENT);

      MapboxNavigationOptions options = navigation.options();
      // Set sdkIdentifier based on if from UI or not
      String sdkIdentifier = updateSdkIdentifier(options);

      NavigationMetricsWrapper.sdkIdentifier = sdkIdentifier;
      Event navTurnstileEvent = NavigationMetricsWrapper.turnstileEvent();
      // TODO Check if we are sending two turnstile events (Maps and Nav) and if so, do we want to track them
      // separately?
      NavigationMetricsWrapper.push(navTurnstileEvent);

      isInitialized = true;
    }
    // Setup the listeners
    initEventDispatcherListeners(navigation);
  }

  /**
   * Added once created in the {@link NavigationService}, this class
   * provides data regarding the {@link android.app.Activity} lifecycle.
   *
   * @param application to register the callbacks
   */
  void initializeLifecycleMonitor(Application application) {
    if (lifecycleMonitor == null) {
      lifecycleMonitor = new NavigationLifecycleMonitor(application);
    }
  }

  /**
   * Called when navigation is starting for the first time.
   * Initializes the {@link SessionState}.
   *
   * @param directionsRoute first route passed to navigation
   */
  void startSession(DirectionsRoute directionsRoute) {
    if (!isConfigurationChange) {
      navigationSessionState = navigationSessionState.toBuilder()
        .sessionIdentifier(TelemetryUtils.obtainUniversalUniqueIdentifier())
        .originalDirectionRoute(directionsRoute)
        .originalRequestIdentifier(directionsRoute.routeOptions().requestUuid())
        .requestIdentifier(directionsRoute.routeOptions().requestUuid())
        .currentDirectionRoute(directionsRoute)
        .eventRouteDistanceCompleted(0)
        .mockLocation(metricLocation.getLocation().getProvider().equals(MOCK_PROVIDER))
        .rerouteCount(0)
        .build();
    }
    isConfigurationChange = false;
  }

  /**
   * Flushes any remaining events from the reroute / feedback queue and fires
   * a cancel event indicating a terminated session.
   */
  void endSession(boolean isConfigurationChange) {
    this.isConfigurationChange = isConfigurationChange;
    if (!isConfigurationChange) {
      if (navigationSessionState.startTimestamp() != null) {
        flushEventQueues();
        updateLifecyclePercentages();
        NavigationMetricsWrapper.cancelEvent(navigationSessionState, metricProgress, metricLocation.getLocation());
      }
      lifecycleMonitor = null;
      NavigationMetricsWrapper.disable();
      isInitialized = false;
    }
  }

  /**
   * Called when a new {@link DirectionsRoute} is given in
   * {@link MapboxNavigation#startNavigation(DirectionsRoute)}.
   * <p>
   * At this point, navigation has already begun and the {@link SessionState}
   * needs to be updated.
   *
   * @param directionsRoute new route passed to {@link MapboxNavigation}
   */
  void updateSessionRoute(DirectionsRoute directionsRoute) {
    SessionState.Builder navigationBuilder = navigationSessionState.toBuilder();
    navigationBuilder.currentDirectionRoute(directionsRoute);
    eventDispatcher.addMetricEventListeners(this);

    if (isOffRoute) {
      // If we are off-route, update the reroute count
      navigationBuilder.rerouteCount(navigationSessionState.rerouteCount() + 1);
      boolean hasRouteOptions = directionsRoute.routeOptions() != null;
      navigationBuilder.requestIdentifier(hasRouteOptions ? directionsRoute.routeOptions().requestUuid() : null);
      navigationSessionState = navigationBuilder.build();

      updateLastRerouteEvent(directionsRoute);
      lastRerouteDate = new Date();
      isOffRoute = false;
    } else {
      // Not current off-route - just update the session
      navigationSessionState = navigationBuilder.build();
    }
  }

  /**
   * Called during {@link NavigationTelemetry#initialize(Context, String, MapboxNavigation, LocationEngine)}
   * and any time {@link MapboxNavigation} gets an updated location engine.
   * <p>
   * Removes the current location engine listener if it exists, then
   * sets up the new one / updates the location engine name.
   *
   * @param locationEngine to be used to update
   */
  void updateLocationEngine(LocationEngine locationEngine) {
    // Remove listener from previous engine
    if (navigationLocationEngine != null) {
      navigationLocationEngine.removeLocationEngineListener(this);
    }

    // Store the new engine and setup a new listener
    if (locationEngine != null) {
      navigationLocationEngine = locationEngine;
      navigationLocationEngine.addLocationEngineListener(this);
      String locationEngineName = locationEngine.getClass().getName();
      navigationSessionState.toBuilder().locationEngineName(locationEngineName);
    }
  }

  /**
   * Creates a new {@link FeedbackEvent} and adds it to the queue
   * of events to be sent.
   *
   * @param feedbackType   defined in FeedbackEvent
   * @param description    optional String describing event
   * @param feedbackSource from either reroute or UI
   * @return String feedbackId to identify the event created if needed
   */
  String recordFeedbackEvent(@FeedbackEvent.FeedbackType String feedbackType, String description,
                             @FeedbackEvent.FeedbackSource String feedbackSource) {
    FeedbackEvent feedbackEvent = queueFeedbackEvent(feedbackType, description, feedbackSource);
    return feedbackEvent.getEventId();
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
   */
  void updateFeedbackEvent(String feedbackId, @FeedbackEvent.FeedbackType String feedbackType,
                           String description, String screenshot) {
    // Find the event and update
    FeedbackEvent feedbackEvent = (FeedbackEvent) findQueuedTelemetryEvent(feedbackId);
    if (feedbackEvent != null) {
      feedbackEvent.setFeedbackType(feedbackType);
      feedbackEvent.setDescription(description);
      feedbackEvent.setScreenshot(screenshot);
    }
  }

  /**
   * Cancels an existing feedback event generated by {@link MapboxNavigation#recordFeedback(String, String, String)}.
   * <p>
   * Uses a feedback ID to find the correct event and then cancels it (will no longer be recorded).
   *
   * @param feedbackId generated from {@link MapboxNavigation#recordFeedback(String, String, String)}
   */
  void cancelFeedback(String feedbackId) {
    // Find the event and remove it from the queue
    FeedbackEvent feedbackEvent = (FeedbackEvent) findQueuedTelemetryEvent(feedbackId);
    queuedFeedbackEvents.remove(feedbackEvent);
  }

  private void validateAccessToken(String accessToken) {
    if (TextUtils.isEmpty(accessToken) || (!accessToken.toLowerCase(Locale.US).startsWith("pk.")
      && !accessToken.toLowerCase(Locale.US).startsWith("sk."))) {
      throw new NavigationException("A valid access token must be passed in when first initializing"
        + " MapboxNavigation");
    }
  }

  private void initEventDispatcherListeners(MapboxNavigation navigation) {
    eventDispatcher = navigation.getEventDispatcher();
    eventDispatcher.addMetricEventListeners(this);
  }

  @NonNull
  private String updateSdkIdentifier(MapboxNavigationOptions options) {
    String sdkIdentifier = MAPBOX_NAVIGATION_SDK_IDENTIFIER;
    if (options.isFromNavigationUi()) {
      sdkIdentifier = MAPBOX_NAVIGATION_UI_SDK_IDENTIFIER;
    }
    return sdkIdentifier;
  }

  private void flushEventQueues() {
    for (FeedbackEvent feedbackEvent : queuedFeedbackEvents) {
      sendFeedbackEvent(feedbackEvent);
    }
    for (RerouteEvent rerouteEvent : queuedRerouteEvents) {
      sendRerouteEvent(rerouteEvent);
    }
  }

  private void updateCurrentLocation(Location rawLocation) {
    metricLocation = new MetricsLocation(rawLocation);
    locationBuffer.addLast(rawLocation);

    // Check queued reroute events
    checkRerouteQueue();
    // Check queued feedback events
    checkFeedbackQueue();
  }

  private void checkRerouteQueue() {
    Iterator<RerouteEvent> iterator = queuedRerouteEvents.listIterator();
    while (iterator.hasNext()) {
      RerouteEvent rerouteEvent = iterator.next();
      if (shouldSendEvent(rerouteEvent.getSessionState())) {
        sendRerouteEvent(rerouteEvent);
        iterator.remove();
      }
    }
  }

  private void checkFeedbackQueue() {
    Iterator<FeedbackEvent> iterator = queuedFeedbackEvents.listIterator();
    while (iterator.hasNext()) {
      FeedbackEvent feedbackEvent = iterator.next();
      if (shouldSendEvent(feedbackEvent.getSessionState())) {
        sendFeedbackEvent(feedbackEvent);
        iterator.remove();
      }
    }
  }

  private boolean shouldSendEvent(SessionState sessionState) {
    return dateDiff(sessionState.eventDate(), new Date(), TimeUnit.SECONDS) > TWENTY_SECOND_INTERVAL;
  }

  @NonNull
  private List<Location> createLocationListBeforeEvent(Date eventDate) {
    Location[] locations = locationBuffer.toArray(new Location[locationBuffer.size()]);
    // Create current list of dates
    List<Location> currentLocationList = Arrays.asList(locations);
    // Setup list for dates before the event
    List<Location> locationsBeforeEvent = new ArrayList<>();
    // Add any events before the event date
    for (Location location : currentLocationList) {
      Date locationDate = new Date(location.getTime());
      if (locationDate.before(eventDate)) {
        locationsBeforeEvent.add(location);
      }
    }
    return locationsBeforeEvent;
  }

  @NonNull
  private List<Location> createLocationListAfterEvent(Date eventDate) {
    Location[] locations = locationBuffer.toArray(new Location[locationBuffer.size()]);
    // Create current list of dates
    List<Location> currentLocationList = Arrays.asList(locations);
    // Setup list for dates after the event
    List<Location> locationsAfterEvent = new ArrayList<>();
    // Add any events after the event date
    for (Location location : currentLocationList) {
      Date locationDate = new Date(location.getTime());
      if (locationDate.after(eventDate)) {
        locationsAfterEvent.add(location);
      }
    }
    return locationsAfterEvent;
  }

  private void updateDistanceCompleted() {
    double currentDistanceCompleted = navigationSessionState.eventRouteDistanceCompleted()
      + metricProgress.getDistanceTraveled();
    navigationSessionState = navigationSessionState.toBuilder()
      .eventRouteDistanceCompleted(currentDistanceCompleted)
      .build();
  }

  private void queueRerouteEvent() {
    updateLifecyclePercentages();
    // Create a new session state given the current navigation session
    Date eventDate = new Date();
    SessionState rerouteEventSessionState = navigationSessionState.toBuilder()
      .eventDate(eventDate)
      .eventRouteProgress(metricProgress)
      .eventLocation(metricLocation.getLocation())
      .secondsSinceLastReroute(getSecondsSinceLastReroute(eventDate))
      .mockLocation(metricLocation.getLocation().getProvider().equals(MOCK_PROVIDER))
      .build();

    RerouteEvent rerouteEvent = new RerouteEvent(rerouteEventSessionState);
    queuedRerouteEvents.add(rerouteEvent);
  }

  @NonNull
  private FeedbackEvent queueFeedbackEvent(@FeedbackEvent.FeedbackType String feedbackType,
                                           String description, @FeedbackEvent.FeedbackSource String feedbackSource) {
    updateLifecyclePercentages();
    // Distance completed = previous distance completed + current RouteProgress distance traveled
    double distanceCompleted = navigationSessionState.eventRouteDistanceCompleted()
      + metricProgress.getDistanceTraveled();

    // Create a new session state given the current navigation session
    SessionState feedbackEventSessionState = navigationSessionState.toBuilder()
      .eventDate(new Date())
      .eventRouteProgress(metricProgress)
      .eventRouteDistanceCompleted(distanceCompleted)
      .eventLocation(metricLocation.getLocation())
      .mockLocation(metricLocation.getLocation().getProvider().equals(MOCK_PROVIDER))
      .build();

    FeedbackEvent feedbackEvent = new FeedbackEvent(feedbackEventSessionState, feedbackSource);
    feedbackEvent.setDescription(description);
    feedbackEvent.setFeedbackType(feedbackType);
    queuedFeedbackEvents.add(feedbackEvent);
    return feedbackEvent;
  }

  private void sendRerouteEvent(RerouteEvent rerouteEvent) {
    // If there isn't an updated geometry, don't send
    if (rerouteEvent.getNewRouteGeometry() == null
      || rerouteEvent.getSessionState().startTimestamp() == null) {
      return;
    }
    // Create arrays with locations from before / after the reroute occurred
    List<Location> beforeLocations = createLocationListBeforeEvent(rerouteEvent.getSessionState().eventDate());
    List<Location> afterLocations = createLocationListAfterEvent(rerouteEvent.getSessionState().eventDate());
    // Update session state with locations after feedback
    SessionState rerouteSessionState = rerouteEvent.getSessionState().toBuilder()
      .beforeEventLocations(beforeLocations)
      .afterEventLocations(afterLocations)
      .build();
    // Set the updated session state
    rerouteEvent.setRerouteSessionState(rerouteSessionState);

    NavigationMetricsWrapper.rerouteEvent(rerouteEvent, metricProgress,
      rerouteEvent.getSessionState().eventLocation());
  }

  private void sendFeedbackEvent(FeedbackEvent feedbackEvent) {
    if (feedbackEvent.getSessionState().startTimestamp() == null) {
      return;
    }
    // Create arrays with locations from before / after the reroute occurred
    List<Location> beforeLocations = createLocationListBeforeEvent(feedbackEvent.getSessionState().eventDate());
    List<Location> afterLocations = createLocationListAfterEvent(feedbackEvent.getSessionState().eventDate());
    // Update session state with locations after feedback
    SessionState feedbackSessionState = feedbackEvent.getSessionState().toBuilder()
      .beforeEventLocations(beforeLocations)
      .afterEventLocations(afterLocations)
      .build();

    NavigationMetricsWrapper.feedbackEvent(feedbackSessionState, metricProgress,
      feedbackEvent.getSessionState().eventLocation(), feedbackEvent.getDescription(),
      feedbackEvent.getFeedbackType(), feedbackEvent.getScreenshot(), feedbackEvent.getFeedbackSource());
  }

  private long dateDiff(Date firstDate, Date secondDate, TimeUnit timeUnit) {
    long diffInMillis = secondDate.getTime() - firstDate.getTime();
    return timeUnit.convert(diffInMillis, TimeUnit.MILLISECONDS);
  }

  private TelemetryEvent findQueuedTelemetryEvent(String eventId) {
    for (FeedbackEvent feedbackEvent : queuedFeedbackEvents) {
      if (feedbackEvent.getEventId().equals(eventId)) {
        return feedbackEvent;
      }
    }
    for (RerouteEvent rerouteEvent : queuedRerouteEvents) {
      if (rerouteEvent.getEventId().equals(eventId)) {
        return rerouteEvent;
      }
    }
    return null;
  }

  private void updateLifecyclePercentages() {
    if (lifecycleMonitor != null) {
      navigationSessionState = navigationSessionState.toBuilder()
        .percentInForeground(lifecycleMonitor.obtainForegroundPercentage())
        .percentInPortrait(lifecycleMonitor.obtainPortraitPercentage())
        .build();
    }
  }

  private void updateLastRerouteEvent(DirectionsRoute newDirectionsRoute) {
    if (!queuedRerouteEvents.isEmpty()) {
      RerouteEvent rerouteEvent = queuedRerouteEvents.get(queuedRerouteEvents.size() - 1);
      List<Point> geometryPositions = PolylineUtils.decode(newDirectionsRoute.geometry(), Constants.PRECISION_6);
      PolylineUtils.encode(geometryPositions, Constants.PRECISION_5);
      rerouteEvent.setNewRouteGeometry(PolylineUtils.encode(geometryPositions, Constants.PRECISION_5));
      int newDistanceRemaining = newDirectionsRoute.distance() == null ? 0 : newDirectionsRoute.distance().intValue();
      rerouteEvent.setNewDistanceRemaining(newDistanceRemaining);
      int newDurationRemaining = newDirectionsRoute.duration() == null ? 0 : newDirectionsRoute.duration().intValue();
      rerouteEvent.setNewDurationRemaining(newDurationRemaining);
    }
  }

  private int getSecondsSinceLastReroute(Date eventDate) {
    int seconds = -1;
    if (lastRerouteDate == null) {
      return seconds;
    } else {
      long millisSinceLastReroute = eventDate.getTime() - lastRerouteDate.getTime();
      return (int) TimeUnit.MILLISECONDS.toSeconds(millisSinceLastReroute);
    }
  }
}

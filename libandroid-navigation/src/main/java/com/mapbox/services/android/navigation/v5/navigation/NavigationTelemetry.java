package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.directions.v5.models.DirectionsRoute;
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
import com.mapbox.services.android.navigation.v5.utils.time.TimeUtils;
import com.mapbox.services.android.telemetry.MapboxEvent;
import com.mapbox.services.android.telemetry.MapboxTelemetry;
import com.mapbox.services.android.telemetry.constants.TelemetryConstants;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.utils.TelemetryUtils;
import com.mapbox.services.constants.Constants;
import com.mapbox.services.utils.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

class NavigationTelemetry implements LocationEngineListener, NavigationMetricListener {

  private static final String MAPBOX_NAVIGATION_SDK_IDENTIFIER = "mapbox-navigation-android";
  private static final String MAPBOX_NAVIGATION_UI_SDK_IDENTIFIER = "mapbox-navigation-ui-android";
  private static final String MOCK_PROVIDER = "com.mapbox.services.android.navigation.v5.location.MockLocationEngine";
  private static final int TWENTY_SECOND_INTERVAL = 20;

  private List<RerouteEvent> queuedRerouteEvents = new ArrayList<>();
  private List<FeedbackEvent> queuedFeedbackEvents = new ArrayList<>();

  private MetricsRouteProgress metricProgress;
  private MetricsLocation metricLocation;

  private SessionState navigationSessionState;
  private LocationEngine navigationLocationEngine;
  private RingBuffer<Location> locationBuffer;

  private String vendorId;
  private boolean isOffRoute;

  NavigationTelemetry() {
    locationBuffer = new RingBuffer<>(40);
    metricLocation = new MetricsLocation(null);
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
  }

  @Override
  public void onOffRouteEvent(Location offRouteLocation) {
    isOffRoute = true;
    queueRerouteEvent();
  }

  @Override
  public void onDeparture(Location location, RouteProgress routeProgress) {
    if (metricProgress == null) {
      metricProgress = new MetricsRouteProgress(routeProgress);
    }
    NavigationMetricsWrapper.departEvent(navigationSessionState, metricProgress, location);
  }

  @Override
  public void onArrival(Location location, RouteProgress routeProgress) {
    // Update arrival time stamp
    navigationSessionState = navigationSessionState.toBuilder().arrivalTimestamp(new Date()).build();
    // Send arrival event
    NavigationMetricsWrapper.arriveEvent(navigationSessionState, routeProgress, location);
  }

  void initialize(@NonNull Context context, @NonNull String accessToken,
                  MapboxNavigation navigation, LocationEngine locationEngine) {

    // Initial session state
    navigationSessionState = SessionState.builder().build();

    // Setup the location engine
    updateLocationEngine(locationEngine);

    validateAccessToken(accessToken);
    MapboxNavigationOptions options = navigation.options();

    // Set sdkIdentifier based on if from UI or not
    String sdkIdentifier = MAPBOX_NAVIGATION_SDK_IDENTIFIER;
    if (options.isFromNavigationUi()) {
      sdkIdentifier = MAPBOX_NAVIGATION_UI_SDK_IDENTIFIER;
    }
    // Enable extra logging in debug mode
    MapboxTelemetry.getInstance().setDebugLoggingEnabled(options.isDebugLoggingEnabled());

    String userAgent = String.format("%s/%s", sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME);
    MapboxTelemetry.getInstance().initialize(context, accessToken, userAgent, sdkIdentifier,
      BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME);
    MapboxTelemetry.getInstance().newUserAgent(userAgent);

    // Get the current vendorId
    vendorId = obtainVendorId(context);

    NavigationMetricsWrapper.sdkIdentifier = sdkIdentifier;
    NavigationMetricsWrapper.turnstileEvent();
    // TODO This should be removed when we figure out a solution in NavigationTelemetry
    // Force pushing a TYPE_MAP_LOAD event to ensure that the Nav turnstile event is sent
    MapboxTelemetry.getInstance().pushEvent(MapboxEvent.buildMapLoadEvent());
  }

  /**
   * Called when navigation is starting for the first time.
   * Initializes the {@link SessionState}.
   *
   * @param directionsRoute first route passed to navigation
   */
  void startSession(DirectionsRoute directionsRoute) {
    navigationSessionState = navigationSessionState.toBuilder()
      .originalDirectionRoute(directionsRoute)
      .currentDirectionRoute(directionsRoute)
      .sessionIdentifier(TelemetryUtils.buildUUID())
      .eventRouteDistanceCompleted(0)
      .startTimestamp(new Date())
      .mockLocation(metricLocation.getLocation().getProvider().equals(MOCK_PROVIDER))
      .rerouteCount(0)
      .build();
  }

  /**
   * Flushes any remaining events from the reroute / feedback queue and fires
   * a cancel event indicating a terminated session.
   */
  void endSession() {
    flushEventQueues();
    NavigationMetricsWrapper.cancelEvent(navigationSessionState, metricProgress, metricLocation.getLocation());
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

    if (isOffRoute) {
      // If we are off-route, update the reroute count
      navigationBuilder.rerouteCount(navigationSessionState.rerouteCount() + 1);
      navigationSessionState = navigationBuilder.build();

      updateLastRerouteEvent(directionsRoute);
      updateLastRerouteDate(new Date());
      isOffRoute = false;
    } else {
      // Not current off-route - just update the session
      navigationSessionState = navigationBuilder.build();
    }
  }

  private void updateLastRerouteEvent(DirectionsRoute newDirectionsRoute) {
    RerouteEvent rerouteEvent = queuedRerouteEvents.get(queuedRerouteEvents.size() - 1);
    List<Point> geometryPositions = PolylineUtils.decode(newDirectionsRoute.geometry(), Constants.PRECISION_6);
    PolylineUtils.encode(geometryPositions, Constants.PRECISION_5);
    rerouteEvent.setNewRouteGeometry(PolylineUtils.encode(geometryPositions, Constants.PRECISION_5));
    int newDistanceRemaining = newDirectionsRoute.distance() == null ? 0 : newDirectionsRoute.distance().intValue();
    rerouteEvent.setNewDistanceRemaining(newDistanceRemaining);
    int newDurationRemaining = newDirectionsRoute.duration() == null ? 0 : newDirectionsRoute.duration().intValue();
    rerouteEvent.setNewDurationRemaining(newDurationRemaining);
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
   */
  void updateFeedbackEvent(String feedbackId, @FeedbackEvent.FeedbackType String feedbackType,
                           String description) {
    // Find the event and update
    FeedbackEvent feedbackEvent = (FeedbackEvent) findQueuedTelemetryEvent(feedbackId);
    if (feedbackEvent != null) {
      feedbackEvent.setFeedbackType(feedbackType);
      feedbackEvent.setDescription(description);
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

  private void flushEventQueues() {
    for (FeedbackEvent feedbackEvent : queuedFeedbackEvents) {
      sendFeedbackEvent(feedbackEvent);
    }
    for (RerouteEvent rerouteEvent : queuedRerouteEvents) {
      sendRerouteEvent(rerouteEvent);
    }
  }

  private String obtainVendorId(Context context) {
    SharedPreferences prefs = TelemetryUtils.getSharedPreferences(context.getApplicationContext());
    return prefs.getString(TelemetryConstants.MAPBOX_SHARED_PREFERENCE_KEY_VENDOR_ID, "");
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
    return TimeUtils.dateDiff(sessionState.eventDate(), new Date(), TimeUnit.SECONDS) > TWENTY_SECOND_INTERVAL;
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

  private void queueRerouteEvent() {
    // Distance completed = previous distance completed + current RouteProgress distance traveled
    double distanceCompleted = navigationSessionState.eventRouteDistanceCompleted()
      + metricProgress.getDistanceTraveled();

    // Create a new session state given the current navigation session
    SessionState rerouteEventSessionState = navigationSessionState.toBuilder()
      .eventDate(new Date())
      .eventRouteProgress(metricProgress)
      .eventRouteDistanceCompleted(distanceCompleted)
      .eventLocation(metricLocation.getLocation())
      .mockLocation(metricLocation.getLocation().getProvider().equals(MOCK_PROVIDER))
      .build();

    RerouteEvent rerouteEvent = new RerouteEvent(rerouteEventSessionState);
    queuedRerouteEvents.add(rerouteEvent);
  }

  @NonNull
  private FeedbackEvent queueFeedbackEvent(@FeedbackEvent.FeedbackType String feedbackType,
                                           String description, @FeedbackEvent.FeedbackSource String feedbackSource) {
    // Create a new session state given the current navigation session
    SessionState feedbackEventSessionState = navigationSessionState.toBuilder()
      .eventDate(new Date())
      .eventRouteProgress(metricProgress)
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
      feedbackEvent.getFeedbackType(), "", feedbackEvent.getEventId(), vendorId);
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

  private void updateLastRerouteDate(Date date) {
    for (RerouteEvent rerouteEvent : queuedRerouteEvents) {
      rerouteEvent.getSessionState().toBuilder().lastRerouteDate(date).build();
    }
  }
}

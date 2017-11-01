package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.core.location.LocationEngine;
import com.mapbox.services.android.core.location.LocationEngineListener;
import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;
import com.mapbox.services.android.navigation.v5.utils.time.TimeUtils;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.VOICE_INSTRUCTION_MILESTONE_ID;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_NOTIFICATION_ID;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.buildInstructionString;

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
  private static final int TWENTY_SECOND_INTERVAL = 20;
  private static final String MOCK_PROVIDER = "com.mapbox.services.android.navigation.v5.location.MockLocationEngine";

  private RingBuffer<Integer> recentDistancesFromManeuverInMeters;
  private final IBinder localBinder = new LocalBinder();
  private NavigationNotification navNotificationManager;
  private List<SessionState> queuedRerouteEvents;
  private List<FeedbackEvent> queuedFeedbackEvents;
  private RingBuffer<Location> locationBuffer;
  private long timeIntervalSinceLastOffRoute;
  private MapboxNavigation mapboxNavigation;
  private LocationEngine locationEngine;
  private String locationEngineName;
  private RouteProgress routeProgress;
  private boolean firstProgressUpdate = true;
  private NavigationEngine thread;
  private Location rawLocation;

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
    recentDistancesFromManeuverInMeters = new RingBuffer<>(3);
    locationBuffer = new RingBuffer<>(20);
    queuedFeedbackEvents = new ArrayList<>();
    queuedRerouteEvents = new ArrayList<>();
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
    if (mapboxNavigation.options().enableNotification()) {
      stopForeground(true);
    }

    for (FeedbackEvent feedbackEvent : queuedFeedbackEvents) {
      sendFeedbackEvent(feedbackEvent);
    }
    for (SessionState sessionState : queuedRerouteEvents) {
      sendRerouteEvent(sessionState);
    }

    // User canceled navigation session
    if (routeProgress != null && rawLocation != null) {
      NavigationMetricsWrapper.cancelEvent(mapboxNavigation.getSessionState(), routeProgress,
        rawLocation, locationEngineName);
    }
    endNavigation();
    super.onDestroy();
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
    navNotificationManager = new NavigationNotification(this, mapboxNavigation);
    Notification notifyBuilder
      = navNotificationManager.buildPersistentNotification(R.layout.layout_notification_default,
      R.layout.layout_notification_default_big);

    notifyBuilder.flags = Notification.FLAG_FOREGROUND_SERVICE;
    startForeground(NAVIGATION_NOTIFICATION_ID, notifyBuilder);
  }

  /**
   * Specifically removes this locationEngine listener which was added at the very beginning, quits
   * the thread, and finally stops this service from running in the background.
   */
  void endNavigation() {
    locationEngine.removeLocationEngineListener(this);
    if (navNotificationManager != null) {
      navNotificationManager.unregisterReceiver();
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      thread.quitSafely();
    } else {
      thread.quit();
    }
  }

  /**
   * Location engine already checks if the listener isn't already added so no need to check here.
   * If the user decides to call {@link MapboxNavigation#setLocationEngine(LocationEngine)} during
   * the navigation session, this gets called again in order to attach the location listener to the
   * new engine.
   */
  void acquireLocationEngine() {
    locationEngine = mapboxNavigation.getLocationEngine();
    locationEngine.addLocationEngineListener(this);
    locationEngineName = obtainLocationEngineName();
  }

  /**
   * At the very beginning of navigation session, a forced location update occurs so that the
   * developer can immediately get a routeProgress object to display information.
   */
  @SuppressWarnings("MissingPermission")
  private void forceLocationUpdate() {
    Location lastLocation = locationEngine.getLastLocation();
    if (lastLocation != null) {
      rawLocation = lastLocation;
      thread.queueTask(MSG_LOCATION_UPDATED, NewLocationModel.create(lastLocation, mapboxNavigation,
        recentDistancesFromManeuverInMeters));
    }
  }

  @Override
  @SuppressWarnings("MissingPermission")
  public void onConnected() {
    Timber.d("NavigationService now connected to rawLocation listener.");
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    Timber.d("onLocationChanged");
    if (location != null && validLocationUpdate(location)) {
      rawLocation = location;
      locationBuffer.addLast(location);
      thread.queueTask(MSG_LOCATION_UPDATED, NewLocationModel.create(location, mapboxNavigation,
        recentDistancesFromManeuverInMeters));

      updateRerouteQueue(locationBuffer);
      updateFeedbackQueue(locationBuffer);
    }
  }


  /**
   * Runs several checks on the actual rawLocation object itself in order to ensure that we are
   * performing navigation progress on a accurate/valid rawLocation update.
   */
  @SuppressWarnings("MissingPermission")
  private boolean validLocationUpdate(Location location) {
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

    if (firstProgressUpdate) {
      NavigationMetricsWrapper.departEvent(mapboxNavigation.getSessionState(), routeProgress,
        rawLocation, locationEngineName);
      firstProgressUpdate = false;
    }
    if (mapboxNavigation.options().enableNotification()) {
      navNotificationManager.updateDefaultNotification(routeProgress);
    }
    mapboxNavigation.getEventDispatcher().onProgressChange(location, routeProgress);
  }

  /**
   * With each valid and successful rawLocation update, this will get called once the work on the
   * navigation engine thread has finished. Depending on whether or not a milestone gets triggered
   * or not, the navigation event dispatcher will be called to notify the developer.
   */
  @Override
  public void onMilestoneTrigger(List<Milestone> triggeredMilestones, RouteProgress routeProgress) {
    for (Milestone milestone : triggeredMilestones) {
      String instruction = buildInstructionString(routeProgress, milestone);
      if (milestone.getIdentifier() == VOICE_INSTRUCTION_MILESTONE_ID) {
        instruction = ((VoiceInstructionMilestone) milestone).announcement();
      }
      mapboxNavigation.getEventDispatcher().onMilestoneEvent(
        routeProgress, instruction, milestone.getIdentifier());
    }
  }

  /**
   * With each valid and successful rawLocation update, this callback gets invoked and depending on
   * whether or not the user is off route, the event dispatcher gets called.
   */
  @Override
  public void onUserOffRoute(Location location, boolean userOffRoute) {
    if (userOffRoute) {
      if (location.getTime() > timeIntervalSinceLastOffRoute
        + TimeUnit.SECONDS.toMillis(mapboxNavigation.options().secondsBeforeReroute())) {
        timeIntervalSinceLastOffRoute = location.getTime();
        if (mapboxNavigation.getSessionState().eventLocation() == null) {
          rerouteSessionsStateUpdate();
        } else {
          Point lastReroutePoint = Point.fromLngLat(
            mapboxNavigation.getSessionState().eventLocation().getLongitude(),
            mapboxNavigation.getSessionState().eventLocation().getLatitude());
          if (TurfMeasurement.distance(lastReroutePoint,
            Point.fromLngLat(location.getLongitude(), location.getLatitude()),
            TurfConstants.UNIT_METERS)
            > mapboxNavigation.options().minimumDistanceBeforeRerouting()) {
            rerouteSessionsStateUpdate();
          }
        }
      }
    } else {
      timeIntervalSinceLastOffRoute = location.getTime();
    }
  }

  private void rerouteSessionsStateUpdate() {
    recentDistancesFromManeuverInMeters.clear();
    mapboxNavigation.getEventDispatcher().onUserOffRoute(rawLocation);
    mapboxNavigation.setSessionState(
      mapboxNavigation.getSessionState().toBuilder().eventLocation(rawLocation).build());
  }

  public void rerouteOccurred() {
    mapboxNavigation.setSessionState(mapboxNavigation.getSessionState().toBuilder()
      .routeProgressBeforeReroute(routeProgress)
      .beforeRerouteLocations(Arrays.asList(
        locationBuffer.toArray(new Location[locationBuffer.size()])))
      .previousRouteDistancesCompleted(
        mapboxNavigation.getSessionState().previousRouteDistancesCompleted()
          + routeProgress.distanceTraveled())
      .rerouteDate(new Date())
      .build());
    queuedRerouteEvents.add(mapboxNavigation.getSessionState());
  }

  public String recordFeedbackEvent(String feedbackType, String description,
                                    @FeedbackEvent.FeedbackSource String feedbackSource) {
    // Get current session state and update with "before" locations (equal to current state of the location buffer)
    SessionState feedbackEventSessionState = mapboxNavigation.getSessionState().toBuilder()
      .rerouteDate(new Date())
      .beforeRerouteLocations(Arrays.asList(
        locationBuffer.toArray(new Location[locationBuffer.size()])))
      .routeProgressBeforeReroute(routeProgress)
      .eventLocation(rawLocation)
      .mockLocation((rawLocation.getProvider().equals(MOCK_PROVIDER)) ? true : false)
      .build();

    FeedbackEvent feedbackEvent = new FeedbackEvent(feedbackEventSessionState, feedbackSource);
    feedbackEvent.setDescription(description);
    feedbackEvent.setFeedbackType(feedbackType);
    queuedFeedbackEvents.add(feedbackEvent);

    return feedbackEvent.getFeedbackId();
  }

  public void updateFeedbackEvent(String feedbackId,
                                  @FeedbackEvent.FeedbackType String feedbackType, String description) {
    FeedbackEvent feedbackEvent = findQueuedFeedbackEvent(feedbackId);
    if (feedbackEvent != null) {
      feedbackEvent.setFeedbackType(feedbackType);
      feedbackEvent.setDescription(description);
    }
  }

  public void cancelFeedback(String feedbackId) {
    FeedbackEvent feedbackEvent = findQueuedFeedbackEvent(feedbackId);
    queuedFeedbackEvents.remove(feedbackEvent);
  }

  void sendFeedbackEvent(FeedbackEvent feedbackEvent) {
    SessionState feedbackSessionState = feedbackEvent.getSessionState();
    feedbackSessionState = feedbackSessionState.toBuilder().afterRerouteLocations(Arrays.asList(
      locationBuffer.toArray(new Location[locationBuffer.size()])))
      .build();

    NavigationMetricsWrapper.feedbackEvent(feedbackSessionState, routeProgress,
      feedbackEvent.getSessionState().eventLocation(), feedbackEvent.getDescription(),
      feedbackEvent.getFeedbackType(), "", feedbackEvent.getFeedbackId(),
      mapboxNavigation.obtainVendorId(), locationEngineName);
  }

  void sendRerouteEvent(SessionState sessionState) {
    sessionState = sessionState.toBuilder()
      .afterRerouteLocations(Arrays.asList(
        locationBuffer.toArray(new Location[locationBuffer.size()])))
      .build();

    NavigationMetricsWrapper.rerouteEvent(sessionState, routeProgress,
      sessionState.eventLocation(), locationEngineName);

    for (SessionState session : queuedRerouteEvents) {
      queuedRerouteEvents.set(queuedRerouteEvents.indexOf(session),
        session.toBuilder().lastRerouteDate(
          sessionState.rerouteDate()
        ).build());
    }

    mapboxNavigation.setSessionState(mapboxNavigation.getSessionState().toBuilder().lastRerouteDate(
      sessionState.rerouteDate()
    ).build());
  }

  class LocalBinder extends Binder {
    NavigationService getService() {
      Timber.d("Local binder called.");
      return NavigationService.this;
    }
  }

  private FeedbackEvent findQueuedFeedbackEvent(String feedbackId) {
    for (FeedbackEvent feedbackEvent : queuedFeedbackEvents) {
      if (feedbackEvent.getFeedbackId().equals(feedbackId)) {
        return feedbackEvent;
      }
    }
    return null;
  }

  private void updateFeedbackQueue(RingBuffer locationBuffer) {
    Iterator<FeedbackEvent> iterator = queuedFeedbackEvents.listIterator();
    while (iterator.hasNext()) {
      FeedbackEvent feedbackEvent = iterator.next();
      if (feedbackEvent.getSessionState().eventLocation() != null
        && feedbackEvent.getSessionState().eventLocation().equals(locationBuffer.peekFirst())
        || TimeUtils.dateDiff(feedbackEvent.getSessionState().rerouteDate(), new Date(), TimeUnit.SECONDS)
        > TWENTY_SECOND_INTERVAL) {
        sendFeedbackEvent(feedbackEvent);
        iterator.remove();
      }
    }
  }

  private void updateRerouteQueue(RingBuffer locationBuffer) {
    Iterator<SessionState> iterator = queuedRerouteEvents.listIterator();
    while (iterator.hasNext()) {
      SessionState sessionState = iterator.next();
      if (sessionState.eventLocation() != null
        && sessionState.eventLocation().equals(locationBuffer.peekFirst())
        || TimeUtils.dateDiff(sessionState.rerouteDate(), new Date(), TimeUnit.SECONDS)
        > TWENTY_SECOND_INTERVAL) {
        sendRerouteEvent(sessionState);
        iterator.remove();
      }
    }
  }

  private String obtainLocationEngineName() {
    return locationEngine.getClass().getSimpleName();
  }
}

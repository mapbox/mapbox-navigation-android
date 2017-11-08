package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.location.MetricsLocation;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;
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

class NavigationQueueContainer {
  private static final int TWENTY_SECOND_INTERVAL = 20;
  private static final String MOCK_PROVIDER = "com.mapbox.services.android.navigation.v5.location.MockLocationEngine";

  private List<SessionState> queuedRerouteEvents = new ArrayList<>();
  private List<FeedbackEvent> queuedFeedbackEvents = new ArrayList<>();
  private MapboxNavigation mapboxNavigation;
  private String locationEngineName;
  private MetricsRouteProgress routeProgress;
  private MetricsLocation currentLocation;
  private RingBuffer<Location> locationBuffer;
  private boolean firstProgressUpdate = true;
  private long timeIntervalSinceLastOffRoute;

  NavigationQueueContainer(MapboxNavigation mapboxNavigation) {
    this.mapboxNavigation = mapboxNavigation;
    locationBuffer = new RingBuffer<>(20);
  }

  void updateCurrentLocation(Location rawLocation) {
    this.currentLocation = new MetricsLocation(rawLocation);
    locationBuffer.addLast(rawLocation);

    // Update queues with new location
    updateRerouteQueue();
    updateFeedbackQueue();
  }

  void sendQueues() {
    for (FeedbackEvent feedbackEvent : queuedFeedbackEvents) {
      sendFeedbackEvent(feedbackEvent);
    }
    for (SessionState sessionState : queuedRerouteEvents) {
      sendRerouteEvent(sessionState);
    }
  }

  void rerouteSessionsStateUpdate() {
    mapboxNavigation.getEventDispatcher().onUserOffRoute(currentLocation.getLocation());
    mapboxNavigation.setSessionState(mapboxNavigation.getSessionState().toBuilder()
      .eventLocation(currentLocation.getLocation()).build());
  }

  void rerouteOccurred() {
    mapboxNavigation.setSessionState(mapboxNavigation.getSessionState().toBuilder()
      .routeProgressBeforeReroute(routeProgress)
      .beforeRerouteLocations(Arrays.asList(
        locationBuffer.toArray(new Location[locationBuffer.size()])))
      .previousRouteDistancesCompleted(
        mapboxNavigation.getSessionState().previousRouteDistancesCompleted()
          + routeProgress.getDistanceTraveled())
      .rerouteDate(new Date())
      .build());
    queuedRerouteEvents.add(mapboxNavigation.getSessionState());
  }

  String recordFeedbackEvent(String feedbackType, String description,
                             @FeedbackEvent.FeedbackSource String feedbackSource) {
    SessionState feedbackEventSessionState = mapboxNavigation.getSessionState().toBuilder()
      .rerouteDate(new Date())
      .beforeRerouteLocations(Arrays.asList(
        locationBuffer.toArray(new Location[locationBuffer.size()])))
      .routeProgressBeforeReroute(routeProgress)
      .eventLocation(currentLocation.getLocation())
      .mockLocation(currentLocation.getLocation().getProvider().equals(MOCK_PROVIDER))
      .build();

    FeedbackEvent feedbackEvent = new FeedbackEvent(feedbackEventSessionState, feedbackSource);
    feedbackEvent.setDescription(description);
    feedbackEvent.setFeedbackType(feedbackType);
    queuedFeedbackEvents.add(feedbackEvent);

    return feedbackEvent.getFeedbackId();
  }

  void updateFeedbackEvent(String feedbackId,
                           @FeedbackEvent.FeedbackType String feedbackType, String description) {
    FeedbackEvent feedbackEvent = findQueuedFeedbackEvent(feedbackId);
    if (feedbackEvent != null) {
      feedbackEvent.setFeedbackType(feedbackType);
      feedbackEvent.setDescription(description);
    }
  }

  void cancelFeedback(String feedbackId) {
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

  private FeedbackEvent findQueuedFeedbackEvent(String feedbackId) {
    for (FeedbackEvent feedbackEvent : queuedFeedbackEvents) {
      if (feedbackEvent.getFeedbackId().equals(feedbackId)) {
        return feedbackEvent;
      }
    }
    return null;
  }

  private void updateFeedbackQueue() {
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

  private void updateRerouteQueue() {
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

  void setLocationEngineName(String locationEngineName) {
    this.locationEngineName = locationEngineName;
  }

  void setRouteProgress(RouteProgress routeProgress) {
    this.routeProgress = new MetricsRouteProgress(routeProgress);

    if (firstProgressUpdate) {
      NavigationMetricsWrapper.departEvent(mapboxNavigation.getSessionState(), this.routeProgress,
        currentLocation.getLocation(), locationEngineName);
      firstProgressUpdate = false;
    }
  }

  void cancelNavigationSession() {
    if (routeProgress != null && currentLocation != null) {
      NavigationMetricsWrapper.cancelEvent(mapboxNavigation.getSessionState(), routeProgress,
        currentLocation.getLocation(), locationEngineName);
    }
  }

  void onUserOffRoute(Location location, boolean userOffRoute) {
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
}

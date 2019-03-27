package com.mapbox.services.android.navigation.v5.internal.navigation;

import com.mapbox.services.android.navigation.v5.internal.location.MetricsLocation;
import com.mapbox.services.android.navigation.v5.navigation.metrics.SessionState;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;

import java.util.Date;

class DepartEventFactory {

  private static final int INITIAL_LEG_INDEX = -1;
  private final DepartEventHandler departEventHandler;
  private int currentLegIndex = INITIAL_LEG_INDEX;

  DepartEventFactory(DepartEventHandler departEventHandler) {
    this.departEventHandler = departEventHandler;
  }

  SessionState send(SessionState sessionState, MetricsRouteProgress routeProgress, MetricsLocation location) {
    sessionState = checkResetForNewLeg(sessionState, routeProgress);
    this.currentLegIndex = routeProgress.getLegIndex();
    if (isValidDeparture(sessionState, routeProgress)) {
      return sendToHandler(sessionState, routeProgress, location);
    }
    return sessionState;
  }

  void reset() {
    currentLegIndex = INITIAL_LEG_INDEX;
  }

  private SessionState checkResetForNewLeg(SessionState sessionState, MetricsRouteProgress routeProgress) {
    if (shouldResetDepartureDate(routeProgress)) {
      sessionState = sessionState.toBuilder().startTimestamp(null).build();
    }
    return sessionState;
  }

  private boolean shouldResetDepartureDate(MetricsRouteProgress routeProgress) {
    return currentLegIndex != routeProgress.getLegIndex();
  }

  private boolean isValidDeparture(SessionState sessionState, MetricsRouteProgress routeProgress) {
    return sessionState.startTimestamp() == null && routeProgress.getDistanceTraveled() > 0;
  }

  private SessionState sendToHandler(SessionState sessionState, MetricsRouteProgress routeProgress,
                                     MetricsLocation location) {
    SessionState updatedState = sessionState.toBuilder()
      .startTimestamp(new Date())
      .build();
    departEventHandler.send(updatedState, routeProgress, location);
    return updatedState;
  }
}
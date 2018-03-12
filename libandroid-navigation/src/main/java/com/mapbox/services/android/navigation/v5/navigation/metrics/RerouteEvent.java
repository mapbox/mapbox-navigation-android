package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.support.annotation.NonNull;

import com.mapbox.android.telemetry.TelemetryUtils;

public class RerouteEvent implements TelemetryEvent {

  private SessionState rerouteSessionState;
  private String eventId;
  private String newRouteGeometry;
  private int newDurationRemaining;
  private int newDistanceRemaining;

  public RerouteEvent(SessionState rerouteSessionState) {
    this.rerouteSessionState = rerouteSessionState;
    this.eventId = TelemetryUtils.obtainUniversalUniqueIdentifier();
  }

  @Override
  public String getEventId() {
    return eventId;
  }

  @NonNull
  @Override
  public SessionState getSessionState() {
    return rerouteSessionState;
  }

  public void setRerouteSessionState(SessionState rerouteSessionState) {
    this.rerouteSessionState = rerouteSessionState;
  }

  public String getNewRouteGeometry() {
    return newRouteGeometry;
  }

  public void setNewRouteGeometry(String newRouteGeometry) {
    this.newRouteGeometry = newRouteGeometry;
  }

  public int getNewDurationRemaining() {
    return newDurationRemaining;
  }

  public void setNewDurationRemaining(int newDurationRemaining) {
    this.newDurationRemaining = newDurationRemaining;
  }

  public int getNewDistanceRemaining() {
    return newDistanceRemaining;
  }

  public void setNewDistanceRemaining(int newDistanceRemaining) {
    this.newDistanceRemaining = newDistanceRemaining;
  }
}

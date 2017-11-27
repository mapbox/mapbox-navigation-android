package com.mapbox.services.android.navigation.v5.navigation.metrics;

public interface TelemetryEvent {

  String getEventId();

  SessionState getSessionState();
}

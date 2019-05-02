package com.mapbox.services.android.navigation.v5.internal.navigation.metrics;

public interface TelemetryEvent {

  String getEventId();

  SessionState getSessionState();
}

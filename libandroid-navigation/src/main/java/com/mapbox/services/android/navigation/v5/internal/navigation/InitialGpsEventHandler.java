package com.mapbox.services.android.navigation.v5.internal.navigation;

class InitialGpsEventHandler {
  void send(double elapsedTime, String sessionId) {
    NavigationMetricsWrapper.INSTANCE.sendInitialGpsEvent(elapsedTime, sessionId);
  }
}

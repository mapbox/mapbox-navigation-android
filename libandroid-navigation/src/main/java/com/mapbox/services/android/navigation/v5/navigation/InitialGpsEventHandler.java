package com.mapbox.services.android.navigation.v5.navigation;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationMetricsWrapper.sendInitialGpsEvent;

class InitialGpsEventHandler {

  void send(double elapsedTime, String sessionId, NavigationPerformanceMetadata metadata) {
    sendInitialGpsEvent(elapsedTime, sessionId, metadata);
  }
}

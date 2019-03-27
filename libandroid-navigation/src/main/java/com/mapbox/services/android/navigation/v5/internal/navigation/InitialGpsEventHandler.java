package com.mapbox.services.android.navigation.v5.internal.navigation;

import static com.mapbox.services.android.navigation.v5.internal.navigation.NavigationMetricsWrapper
  .sendInitialGpsEvent;

class InitialGpsEventHandler {

  void send(double elapsedTime, String sessionId) {
    sendInitialGpsEvent(elapsedTime, sessionId);
  }
}

package com.mapbox.services.android.navigation.v5.navigation;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationMetricsWrapper.sendInitialGpsEvent;

class InitialGpsEventHandler {

  void send(double elapsedTime, String sessionId) {
    sendInitialGpsEvent(elapsedTime, sessionId);
  }
}

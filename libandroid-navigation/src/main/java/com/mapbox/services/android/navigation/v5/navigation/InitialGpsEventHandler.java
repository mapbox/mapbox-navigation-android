package com.mapbox.services.android.navigation.v5.navigation;

import android.content.Context;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationMetricsWrapper.sendInitialGpsEvent;

class InitialGpsEventHandler {

  void send(Context context, double elapsedTime, String sessionId,
            NavigationPerformanceMetadata metadata) {
    sendInitialGpsEvent(context, elapsedTime, sessionId, metadata);
  }
}

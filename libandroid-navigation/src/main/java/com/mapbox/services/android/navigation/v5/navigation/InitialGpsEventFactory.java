package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.core.utils.TextUtils;

class InitialGpsEventFactory {

  private static final String EMPTY_STRING = "";
  private String sessionId = EMPTY_STRING;
  private ElapsedTime time;
  private InitialGpsEventHandler handler;
  private boolean hasSent;
  private NavigationPerformanceMetadata metadata;

  InitialGpsEventFactory(NavigationPerformanceMetadata metadata) {
    this(new ElapsedTime(), new InitialGpsEventHandler(), metadata);
  }

  InitialGpsEventFactory(ElapsedTime time, InitialGpsEventHandler handler,
                         NavigationPerformanceMetadata metadata) {
    this.time = time;
    this.handler = handler;
    this.metadata = metadata;
  }

  void navigationStarted(String sessionId) {
    this.sessionId = sessionId;
    time.start();
  }

  void gpsReceived() {
    if (time.getStart() == null) {
      return;
    }
    time.end();
    send(time);
  }

  void reset() {
    time = new ElapsedTime();
    hasSent = false;
  }

  private void send(ElapsedTime time) {
    if (!hasSent && !TextUtils.isEmpty(sessionId)) {
      double elapsedTime = time.getElapsedTime();
      handler.send(elapsedTime, sessionId, metadata);
      hasSent = true;
    }
  }
}

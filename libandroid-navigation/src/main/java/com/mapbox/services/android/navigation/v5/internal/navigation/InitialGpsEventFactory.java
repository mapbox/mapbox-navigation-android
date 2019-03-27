package com.mapbox.services.android.navigation.v5.internal.navigation;

import com.mapbox.core.utils.TextUtils;

class InitialGpsEventFactory {

  private static final String EMPTY_STRING = "";
  private String sessionId = EMPTY_STRING;
  private ElapsedTime time;
  private InitialGpsEventHandler handler;
  private boolean hasSent;

  InitialGpsEventFactory() {
    this(new ElapsedTime(), new InitialGpsEventHandler());
  }

  InitialGpsEventFactory(ElapsedTime time, InitialGpsEventHandler handler) {
    this.time = time;
    this.handler = handler;
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
      handler.send(elapsedTime, sessionId);
      hasSent = true;
    }
  }
}

package com.mapbox.services.android.navigation.v5.internal.navigation;

import java.util.Timer;
import java.util.TimerTask;

class BatteryChargeReporter {

  private static final int NO_DELAY = 0;
  private final Timer timer;
  private final TimerTask task;

  BatteryChargeReporter(Timer timer, TimerTask task) {
    this.timer = timer;
    this.task = task;
  }

  void scheduleAt(long periodInMilliseconds) {
    timer.scheduleAtFixedRate(task, NO_DELAY, periodInMilliseconds);
  }

  void stop() {
    timer.cancel();
  }
}

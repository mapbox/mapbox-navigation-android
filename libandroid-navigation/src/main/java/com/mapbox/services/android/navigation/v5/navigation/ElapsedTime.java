package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.v5.exception.NavigationException;

class ElapsedTime {
  private Long start = null;
  private Long end = null;

  void start() {
    start = System.nanoTime();
  }

  @Nullable
  Long getStart() {
    return start;
  }

  void end() {
    if (start == null) {
      throw new NavigationException("Must call start() before calling end()");
    }
    end = System.nanoTime();
  }

  @Nullable
  Long getEnd() {
    return end;
  }

  long getElapsedTime() {
    if (start == null || end == null) {
      throw new NavigationException("Must call start() and end() before calling getElapsedTime()");
    }
    return end - start;
  }
}

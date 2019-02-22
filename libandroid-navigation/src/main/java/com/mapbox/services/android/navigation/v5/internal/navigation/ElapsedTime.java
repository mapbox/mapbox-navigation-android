package com.mapbox.services.android.navigation.v5.internal.navigation;

import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.v5.internal.exception.NavigationException;

class ElapsedTime {

  private static final double ELAPSED_TIME_DENOMINATOR = 1e+9;
  private static final double PRECISION = 100d;
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

  double getElapsedTime() {
    if (start == null || end == null) {
      throw new NavigationException("Must call start() and end() before calling getElapsedTime()");
    }
    long elapsedTimeInNanoseconds = end - start;
    double elapsedTimeInSeconds = elapsedTimeInNanoseconds / ELAPSED_TIME_DENOMINATOR;
    return Math.round(elapsedTimeInSeconds * PRECISION) / PRECISION;
  }
}

package com.mapbox.services.android.navigation.v5.navigation;

import android.annotation.SuppressLint;
import android.os.Parcelable;

@SuppressLint("ParcelCreator")
@SuppressWarnings("ParcelableCreator")
class InitialGpsEvent extends NavigationPerformanceEvent implements Parcelable {
  private static final String ELAPSED_TIME_NAME = "elapsed_time";

  InitialGpsEvent(double elapsedTime, String sessionId) {
    super(sessionId);
    addCounter(new DoubleCounter(ELAPSED_TIME_NAME, elapsedTime));
  }
}

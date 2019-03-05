package com.mapbox.services.android.navigation.v5.navigation;

import android.annotation.SuppressLint;
import android.os.Parcelable;

@SuppressLint("ParcelCreator")
@SuppressWarnings("ParcelableCreator")
class InitialGpsEvent extends NavigationPerformanceEvent implements Parcelable {
  private static final String TIME_TO_FIRST_GPS = "time_to_first_gps";

  InitialGpsEvent(double elapsedTime, String sessionId) {
    super(sessionId);
    addCounter(new DoubleCounter(TIME_TO_FIRST_GPS, elapsedTime));
  }
}

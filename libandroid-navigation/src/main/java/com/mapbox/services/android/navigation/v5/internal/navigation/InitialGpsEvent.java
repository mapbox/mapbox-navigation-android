package com.mapbox.services.android.navigation.v5.internal.navigation;

import android.annotation.SuppressLint;
import android.os.Parcelable;

@SuppressLint("ParcelCreator")
@SuppressWarnings("ParcelableCreator")
class InitialGpsEvent extends NavigationPerformanceEvent implements Parcelable {
  private static final String TIME_TO_FIRST_GPS = "time_to_first_gps";
  private static final String INITIAL_GPS_EVENT_NAME = "initial_gps_event";

  InitialGpsEvent(double elapsedTime, String sessionId) {
    super(sessionId, INITIAL_GPS_EVENT_NAME, null); // TODO send metadata
    addCounter(new DoubleCounter(TIME_TO_FIRST_GPS, elapsedTime));
  }
}

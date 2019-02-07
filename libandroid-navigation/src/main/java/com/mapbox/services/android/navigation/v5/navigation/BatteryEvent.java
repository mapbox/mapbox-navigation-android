package com.mapbox.services.android.navigation.v5.navigation;

import android.annotation.SuppressLint;
import android.os.Parcelable;

@SuppressLint("ParcelCreator")
@SuppressWarnings("ParcelableCreator")
class BatteryEvent extends NavigationPerformanceEvent implements Parcelable {
  private static final String BATTERY_PERCENTAGE_KEY = "battery_percentage";
  private static final String IS_PLUGGED_IN_KEY = "is_plugged_in";

  BatteryEvent(String sessionId, float batteryPercentage, boolean isPluggedIn) {
    super(sessionId);

    addCounter(new FloatCounter(BATTERY_PERCENTAGE_KEY, batteryPercentage));
    addAttribute(new Attribute(IS_PLUGGED_IN_KEY, String.valueOf(isPluggedIn)));
  }
}

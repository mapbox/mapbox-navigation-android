package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.annotation.SuppressLint;

@SuppressLint("ParcelCreator")
class NavigationArriveEvent extends NavigationEvent {
  private static final String NAVIGATION_ARRIVE = "navigation.arrive";

  NavigationArriveEvent(PhoneState phoneState) {
    super(phoneState);
  }

  @Override
  protected String getEventName() {
    return NAVIGATION_ARRIVE;
  }
}

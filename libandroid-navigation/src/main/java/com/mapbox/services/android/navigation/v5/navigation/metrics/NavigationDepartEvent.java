package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.annotation.SuppressLint;
import android.support.annotation.Keep;

@Keep
@SuppressLint("ParcelCreator")
class NavigationDepartEvent extends NavigationEvent {
  private static final String NAVIGATION_DEPART = "navigation.depart";

  NavigationDepartEvent(PhoneState phoneState) {
    super(phoneState);
  }

  @Override
  protected String getEventName() {
    return NAVIGATION_DEPART;
  }
}
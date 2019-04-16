package com.mapbox.services.android.navigation.v5.navigation.metrics;

import com.google.gson.annotations.JsonAdapter;

public class NavigationRerouteData {
  private NavigationNewData navigationNewData;
  private int secondsSinceLastReroute;

  public NavigationRerouteData(NavigationNewData navigationNewData, int secondsSinceLastReroute) {
    this.navigationNewData = navigationNewData;
    this.secondsSinceLastReroute = secondsSinceLastReroute;
  }

  NavigationNewData getNavigationNewData() {
    return navigationNewData;
  }

  Integer getSecondsSinceLastReroute() {
    return secondsSinceLastReroute;
  }
}

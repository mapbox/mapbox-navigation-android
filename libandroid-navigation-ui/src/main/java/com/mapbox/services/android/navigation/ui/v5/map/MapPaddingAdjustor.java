package com.mapbox.services.android.navigation.ui.v5.map;

import com.mapbox.mapboxsdk.maps.MapboxMap;

public class MapPaddingAdjustor {

  private MapboxMap mapboxMap;
  private int defaultTopPadding;
  private int waynameTopPadding;

  MapPaddingAdjustor(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
  }

  public void calculatePaddingValues(int defaultTopPadding, int waynameLayoutHeight) {
    this.defaultTopPadding = defaultTopPadding;
    this.waynameTopPadding = defaultTopPadding - (waynameLayoutHeight * 2);
  }

  public void updateTopPaddingWithWayname() {
    updateTopPadding(waynameTopPadding);
  }

  public void updateTopPaddingWithDefault() {
    updateTopPadding(defaultTopPadding);
  }

  private void updateTopPadding(int topPadding) {
    mapboxMap.setPadding(0, topPadding, 0, 0);
  }
}

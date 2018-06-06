package com.mapbox.services.android.navigation.ui.v5.map;

import com.mapbox.mapboxsdk.maps.MapboxMap;

class WaynamePaddingAdjustor {

  private MapboxMap mapboxMap;
  private int defaultTopPadding;
  private int waynameTopPadding;

  WaynamePaddingAdjustor(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
  }

  void calculatePaddingValues(int defaultTopPadding, int waynameLayoutHeight) {
    this.defaultTopPadding = defaultTopPadding;
    this.waynameTopPadding = defaultTopPadding - (waynameLayoutHeight * 2);
  }

  void updateTopPaddingWithWayname() {
    updateTopPadding(waynameTopPadding);
  }

  void updateTopPaddingWithDefault() {
    updateTopPadding(defaultTopPadding);
  }

  private void updateTopPadding(int topPadding) {
    mapboxMap.setPadding(0, topPadding, 0, 0);
  }
}

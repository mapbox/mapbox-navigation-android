package com.mapbox.services.android.navigation.ui.v5;

import androidx.annotation.NonNull;

import java.util.HashMap;

class WifiNetworkChecker {

  private final HashMap<Integer, Boolean> statusMap;

  WifiNetworkChecker(HashMap<Integer, Boolean> statusMap) {
    this.statusMap = statusMap;
    initialize(statusMap);
  }

  @NonNull
  Boolean isFast(Integer wifiLevel) {
    Boolean isConnectionFast = statusMap.get(wifiLevel);
    if (isConnectionFast == null) {
      isConnectionFast = false;
    }
    return isConnectionFast;
  }

  private void initialize(HashMap<Integer, Boolean> statusMap) {
    statusMap.put(5, true);
    statusMap.put(4, true);
    statusMap.put(3, true);
    statusMap.put(2, false);
    statusMap.put(1, false);
    statusMap.put(0, false);
  }
}
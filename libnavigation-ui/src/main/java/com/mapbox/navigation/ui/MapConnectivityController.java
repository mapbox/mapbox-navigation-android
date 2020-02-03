package com.mapbox.navigation.ui;

import com.mapbox.mapboxsdk.Mapbox;

class MapConnectivityController {

  void assign(Boolean state) {
    Mapbox.setConnected(state);
  }
}

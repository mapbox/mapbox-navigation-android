package com.mapbox.services.android.navigation.ui.v5.map;

class FpsMap {

  private static final int ZOOM_LEVEL_FIVE = 5;
  private static final int ZOOM_LEVEL_TEN = 10;
  private static final int ZOOM_LEVEL_FIFTEEN = 15;
  private static final int ZOOM_LEVEL_EIGHTEEN = 18;

  private final ThrottleConfig throttleConfig;

  FpsMap(ThrottleConfig throttleConfig) {
    this.throttleConfig = throttleConfig;
  }

  int getFps(double zoom) {
    if (zoom < ZOOM_LEVEL_FIVE) {
      return throttleConfig.getLevels()[0];
    } else if (zoom < ZOOM_LEVEL_TEN) {
      return throttleConfig.getLevels()[1];
    } else if (zoom < ZOOM_LEVEL_FIFTEEN) {
      return throttleConfig.getLevels()[2];
    } else if (zoom < ZOOM_LEVEL_EIGHTEEN) {
      return throttleConfig.getLevels()[3];
    } else {
      return throttleConfig.getLevels()[4];
    }
  }

  ThrottleConfig getThrottleConfig() {
    return throttleConfig;
  }
}

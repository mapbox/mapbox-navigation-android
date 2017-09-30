package com.mapbox.services.android.navigation.ui.v5.camera;

import android.util.SparseArray;

class ScreenDensityMap extends SparseArray<Double> {

  private static final int DENSITY_LOW = 120;
  private static final int DENSITY_MEDIUM = 160;
  private static final int DENSITY_HIGH = 240;
  private static final int DENSITY_260 = 260;
  private static final int DENSITY_280 = 280;
  private static final int DENSITY_300 = 300;
  private static final int DENSITY_XHIGH = 320;
  private static final int DENSITY_340 = 340;
  private static final int DENSITY_360 = 360;
  private static final int DENSITY_400 = 400;
  private static final int DENSITY_420 = 420;
  private static final int DENSITY_XXHIGH = 480;
  private static final int DENSITY_560 = 560;
  private static final int DENSITY_XXXHIGH = 640;

  ScreenDensityMap() {
    put(DENSITY_LOW, 50d);
    put(DENSITY_MEDIUM, 54d);
    put(DENSITY_HIGH, 63d);
    put(DENSITY_260, 68d);
    put(DENSITY_280, 72d);
    put(DENSITY_300, 73d);
    put(DENSITY_XHIGH, 74d);
    put(DENSITY_340, 76d);
    put(DENSITY_360, 78d);
    put(DENSITY_400, 80d);
    put(DENSITY_420, 83d);
    put(DENSITY_XXHIGH, 87d);
    put(DENSITY_560, 100d);
    put(DENSITY_XXXHIGH, 111d);
  }

  double getTargetDistance(int density) {
    if (get(density) == null) {
      return 101d;
    } else {
      return get(density);
    }
  }
}

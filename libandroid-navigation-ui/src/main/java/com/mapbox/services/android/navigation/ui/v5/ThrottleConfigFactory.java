package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.services.android.navigation.ui.v5.map.ThrottleConfig;

public class ThrottleConfigFactory {

  private static final int[] HIGH_EFFICIENCY_LOCATION_LEVELS = new int[]{2, 5, 7, 15, 15};
  private static final int[] DEFAULT_LOCATION_LEVELS = new int[]{3, 5, 0, 15, 25};

  private static final int[] HIGH_EFFICIENCY_MAP_LEVELS = new int[]{10, 10, 10, 10, 15};
  private static final int[] DEFAULT_MAP_LEVELS = new int[]{10, 10, 10, 10, 20};


  public static ThrottleConfig efficiencyLocationProfile() {
    return new ThrottleConfig(ThrottleConfig.ThrottleDomain.LOCATION,
      HIGH_EFFICIENCY_LOCATION_LEVELS);
  }


  public static ThrottleConfig defaultLocationProfile() {
    return new ThrottleConfig(ThrottleConfig.ThrottleDomain.LOCATION,
      DEFAULT_LOCATION_LEVELS);
  }

  public static ThrottleConfig efficiencyMapProfile() {
    return new ThrottleConfig(ThrottleConfig.ThrottleDomain.MAP,
      HIGH_EFFICIENCY_MAP_LEVELS);
  }


  public static ThrottleConfig defaultMapProfile() {
    return new ThrottleConfig(ThrottleConfig.ThrottleDomain.MAP,
      DEFAULT_MAP_LEVELS);
  }
}
package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class OfflineCriteria {

  /**
   * Bicycle type for road bike.
   */
  public static final String ROAD = "Road";

  /**
   * Bicycle type for hybrid bike.
   */
  public static final String HYBRID = "Hybrid";

  /**
   * Bicycle type for city bike.
   */
  public static final String CITY = "City";

  /**
   * Bicycle type for cross bike.
   */
  public static final String CROSS = "Cross";

  /**
   * Bicycle type for mountain bike.
   */
  public static final String MOUNTAIN = "Mountain";

  /**
   * Break waypoint type.
   */
  public static final String BREAK = "break";

  /**
   * Through waypoint type.
   */
  public static final String THROUGH = "through";

  /**
   * Retention policy for the bicycle type parameter in the Directions API.
   */
  @Retention(RetentionPolicy.SOURCE)
  @StringDef( {
    ROAD,
    HYBRID,
    CITY,
    CROSS,
    MOUNTAIN
  })
  public @interface BicycleType {
  }

  /**
   * Retention policy for the waypoint type parameter in the Directions API.
   */
  @Retention(RetentionPolicy.SOURCE)
  @StringDef( {
    BREAK,
    THROUGH
  })
  public @interface WaypointType {
  }
}

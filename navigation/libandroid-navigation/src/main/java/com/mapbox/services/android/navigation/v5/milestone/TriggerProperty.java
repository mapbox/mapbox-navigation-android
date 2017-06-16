package com.mapbox.services.android.navigation.v5.milestone;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The currently support properties used for triggering a milestone.
 *
 * @since 0.4.0
 */
@SuppressWarnings("WeakerAccess") // Public exposed for creation of compound statements outside SDK
public final class TriggerProperty {

  /*
   * Step trigger values
   */

  /**
   * The Milestone will be triggered based on the duration remaining.
   *
   * @since 0.4.0
   */
  public static final int STEP_DURATION_REMAINING = 0x00000000;

  /**
   * The Milestone will be triggered based on the distance remaining.
   *
   * @since 0.4.0
   */
  public static final int STEP_DISTANCE_REMAINING = 0x00000001;

  /**
   * The Milestone will be triggered based on the total step distance.
   *
   * @since 0.4.0
   */
  public static final int STEP_DISTANCE_TOTAL = 0x00000002;

  /**
   * The Milestone will be triggered based on the total step duration.
   *
   * @since 0.4.0
   */
  public static final int STEP_DURATION_TOTAL = 0x00000003;

  /**
   * The Milestone will be triggered based on the current step index.
   *
   * @since 0.4.0
   */
  public static final int STEP_INDEX = 0x00000004;

  public static final int NEW_STEP = 0x00000005;

  /*
   * Leg trigger values
   */

  public static final int LEG_INDEX = 0x00000013;

  /*
   * Route trigger values
   */

  public static final int LAST_STEP = 0x00000006;

  public static final int ROUTE_DEPART = 0x00000007;

  public static final int ROUTE_ARRIVAL = 0x00000008;

  public static final int ROUTE_WAYPOINT = 0x00000009;

  public static final int ROUTE_FRACTION = 0x00000010;

  public static final int ROUTE_DISTANCE = 0x00000011;

  public static final int ROUTE_DURATION = 0x00000012;

  /*
   * ROUTE_DEPART
   *
   * ROUTE_ARRIVAL
   *
   * ROUTE_WAYPOINT
   *
   * ROUTE_FRACTION
   *
   * ROUTE_DISTANCE
   *
   * ROUTE_DURATION
   *
   * LEG_INDEX
   *
   * LEG_FRACTION
   *
   * LEG_DISTANCE
   *
   * LEG_DURATION
   *
   * STEP_FRACTION
   */

  public static final int TRUE = 0x00000124;

  public static final int FALSE = 0x00000100;

  @IntDef( {
    TRUE,
    FALSE
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface BOOLEAN {
  }


}

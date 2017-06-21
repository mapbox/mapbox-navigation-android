package com.mapbox.services.android.navigation.v5.milestone;

/**
 * The currently support properties used for triggering a milestone.
 *
 * @since 0.4.0
 */
@SuppressWarnings("WeakerAccess") // Public exposed for creation of compound statements outside SDK
public final class TriggerProperty {

  /**
   * The Milestone will be triggered based on the duration remaining.
   *
   * @since 0.4.0
   */
  public static final int STEP_DURATION_REMAINING_SECONDS = 0x00000000;

  /**
   * The Milestone will be triggered based on the distance remaining.
   *
   * @since 0.4.0
   */
  public static final int STEP_DISTANCE_REMAINING_METERS = 0x00000001;

  /**
   * The Milestone will be triggered based on the total step distance.
   *
   * @since 0.4.0
   */
  public static final int STEP_DISTANCE_TOTAL_METERS = 0x00000002;

  /**
   * The Milestone will be triggered based on the total step duration.
   *
   * @since 0.4.0
   */
  public static final int STEP_DURATION_TOTAL_SECONDS = 0x00000003;

  /**
   * The Milestone will be triggered based on the current step index.
   *
   * @since 0.4.0
   */
  public static final int STEP_INDEX = 0x00000004;

  public static final int NEW_STEP = 0x00000005;

  public static final int FIRST_STEP = 0x00000008;

  public static final int LAST_STEP = 0x00000006;

  public static final int NEXT_STEP_DISTANCE_METERS = 0x00000007;


  public static final int TRUE = 0x00000124;

  public static final int FALSE = 0x00000100;
}

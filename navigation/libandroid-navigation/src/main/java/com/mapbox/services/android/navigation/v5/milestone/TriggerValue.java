package com.mapbox.services.android.navigation.v5.milestone;

public final class TriggerValue {

  public static final int DURATION = 0x00000004;

  // TODO distance

  /*
   * Step trigger values
   */

  public static final int STEP_DISTANCE = 0x00000000;

  public static final int STEP_DURATION = 0x00000012;

  public static final int STEP_INDEX = 0x00000016;

  public static final int LAST_STEP = 0x00000020;

  /*
   * Leg trigger values
   */

  public static final int NEW_STEP = 0x00000008;

  /*
   * Route trigger values
   */

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


}

package com.mapbox.services.android.navigation.v5.milestone;

import android.util.SparseArray;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

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

  public static final int STEP_DISTANCE_TRAVELED_METERS = 0x00000009;

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

  public static final int NEXT_STEP_DURATION_SECONDS = 0x00000011;

  public static final int FIRST_LEG = 0x00000009;

  public static final int LAST_LEG = 0x000000010;


  public static final int TRUE = 0x00000124;

  public static final int FALSE = 0x00000100;

  static SparseArray<Number[]> getSparseArray(RouteProgress previousRouteProgress, RouteProgress routeProgress) {
    // Build hashMap matching the trigger properties to their corresponding current values.
    SparseArray<Number[]> statementObjects = new SparseArray<>(13);
    statementObjects.put(TriggerProperty.STEP_DISTANCE_TOTAL_METERS,
      new Number[] {routeProgress.currentLegProgress().currentStep().distance()});
    statementObjects.put(TriggerProperty.STEP_DURATION_TOTAL_SECONDS,
      new Number[] {routeProgress.currentLegProgress().currentStep().duration()});
    statementObjects.put(TriggerProperty.STEP_DISTANCE_REMAINING_METERS,
      new Number[] {routeProgress.currentLegProgress().currentStepProgress().distanceRemaining()});
    statementObjects.put(TriggerProperty.STEP_DURATION_REMAINING_SECONDS,
      new Number[] {routeProgress.currentLegProgress().currentStepProgress().durationRemaining()});
    statementObjects.put(TriggerProperty.STEP_DISTANCE_TRAVELED_METERS,
      new Number[] {routeProgress.currentLegProgress().currentStepProgress().distanceTraveled()});
    statementObjects.put(TriggerProperty.STEP_INDEX,
      new Number[] {routeProgress.currentLegProgress().stepIndex()});
    statementObjects.put(TriggerProperty.NEW_STEP,
      new Number[] {
        previousRouteProgress.currentLegProgress().stepIndex(),
        routeProgress.currentLegProgress().stepIndex()});
    statementObjects.put(TriggerProperty.LAST_STEP,
      new Number[] {routeProgress.currentLegProgress().stepIndex(),
        (routeProgress.currentLeg().steps().size() - 2)});
    statementObjects.put(TriggerProperty.FIRST_STEP,
      new Number[] {routeProgress.currentLegProgress().stepIndex(), 0});
    statementObjects.put(TriggerProperty.NEXT_STEP_DURATION_SECONDS,
      new Number[] {
        routeProgress.currentLegProgress().upComingStep() != null
          ? routeProgress.currentLegProgress().upComingStep().duration() : 0});
    statementObjects.put(TriggerProperty.NEXT_STEP_DISTANCE_METERS,
      new Number[] {
        routeProgress.currentLegProgress().upComingStep() != null
          ? routeProgress.currentLegProgress().upComingStep().distance() : 0});
    statementObjects.put(TriggerProperty.FIRST_LEG, new Number[] {routeProgress.legIndex(), 0});
    statementObjects.put(TriggerProperty.LAST_LEG, new Number[] {routeProgress.legIndex(),
      (routeProgress.directionsRoute().legs().size() - 1)});
    return statementObjects;
  }
}

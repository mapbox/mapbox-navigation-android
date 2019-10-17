package com.mapbox.services.android.navigation.v5.milestone

import android.util.SparseArray
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull

/**
 * The currently support properties used for triggering a milestone.
 *
 * @since 0.4.0
 */
object TriggerProperty {

    /**
     * The Milestone will be triggered based on the duration remaining.
     *
     * @since 0.4.0
     */
    const val STEP_DURATION_REMAINING_SECONDS = 0x00000000

    /**
     * The Milestone will be triggered based on the distance remaining.
     *
     * @since 0.4.0
     */
    const val STEP_DISTANCE_REMAINING_METERS = 0x00000001

    /**
     * The Milestone will be triggered based on the total step distance.
     *
     * @since 0.4.0
     */
    const val STEP_DISTANCE_TOTAL_METERS = 0x00000002

    /**
     * The Milestone will be triggered based on the total step duration.
     *
     * @since 0.4.0
     */
    const val STEP_DURATION_TOTAL_SECONDS = 0x00000003

    const val STEP_DISTANCE_TRAVELED_METERS = 0x00000009

    /**
     * The Milestone will be triggered based on the current step index.
     *
     * @since 0.4.0
     */
    const val STEP_INDEX = 0x00000004

    const val NEW_STEP = 0x00000005

    const val FIRST_STEP = 0x00000008

    const val LAST_STEP = 0x00000006

    const val NEXT_STEP_DISTANCE_METERS = 0x00000007

    const val NEXT_STEP_DURATION_SECONDS = 0x00000011

    const val FIRST_LEG = 0x00000009

    const val LAST_LEG = 0x000000010

    const val TRUE = 0x00000124

    const val FALSE = 0x00000100

    private const val STATEMENTS_COUNT = 13

    @JvmStatic
    fun getSparseArray(
        previousRouteProgress: RouteProgress,
        routeProgress: RouteProgress
    ): SparseArray<Array<Number>> =
        // Build hashMap matching the trigger properties to their corresponding current values.
        SparseArray<Array<Number>>(STATEMENTS_COUNT).apply {
            ifNonNull(routeProgress.currentLegProgress()?.currentStep()) { currentStep ->
                put(
                    STEP_DISTANCE_TOTAL_METERS,
                    arrayOf(currentStep.distance())
                )
                put(
                    STEP_DURATION_TOTAL_SECONDS,
                    arrayOf(currentStep.duration())
                )
            }
            ifNonNull(routeProgress.currentLegProgress()?.currentStepProgress()) { currentStepProgress ->
                put(
                    STEP_DISTANCE_REMAINING_METERS,
                    arrayOf(currentStepProgress.distanceRemaining() as Number)
                )
                put(
                    STEP_DURATION_REMAINING_SECONDS,
                    arrayOf(currentStepProgress.durationRemaining() as Number)
                )
                put(
                    STEP_DISTANCE_TRAVELED_METERS,
                    arrayOf(currentStepProgress.distanceTraveled() as Number)
                )
            }
            ifNonNull(routeProgress.currentLegProgress()) { currentLegProgress ->
                put(
                    STEP_INDEX,
                    arrayOf(currentLegProgress.stepIndex() as Number)
                )
                put(
                    NEW_STEP,
                    arrayOf(
                        currentLegProgress.stepIndex() as Number,
                        currentLegProgress.stepIndex() as Number
                    )
                )
                put(
                    LAST_STEP,
                    arrayOf(
                        currentLegProgress.stepIndex() as Number,
                        ifNonNull(routeProgress.currentLeg()?.steps()) {
                            it.size - 2
                        } ?: 0
                    )
                )
                put(
                    FIRST_STEP,
                    arrayOf(currentLegProgress.stepIndex() as Number, 0)
                )
                put(
                    NEXT_STEP_DURATION_SECONDS,
                    arrayOf(
                        ifNonNull(currentLegProgress.upComingStep()) {
                            it.duration()
                        } ?: 0.0
                    )
                )
                put(
                    NEXT_STEP_DISTANCE_METERS,
                    arrayOf(
                        ifNonNull(currentLegProgress.upComingStep()) {
                            it.distance()
                        } ?: 0.0
                    )
                )
            }
            ifNonNull(routeProgress, routeProgress.legIndex()) { routeProgress, legIndex ->
                put(
                    FIRST_LEG,
                    arrayOf(legIndex, 0)
                )
            }

            ifNonNull(
                routeProgress.directionsRoute(),
                routeProgress.legIndex()
            ) { directionsRoute, legIndex ->
                put(
                    LAST_LEG,
                    arrayOf(
                        legIndex,
                        ifNonNull(directionsRoute.legs()) {
                            it.size - 1
                        } ?: 0
                    )
                )
            }
        }
}

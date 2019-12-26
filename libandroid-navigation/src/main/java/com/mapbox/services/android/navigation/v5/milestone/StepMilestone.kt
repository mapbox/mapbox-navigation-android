package com.mapbox.services.android.navigation.v5.milestone

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull

/**
 * Using a Step Milestone will result in
 * [MilestoneEventListener.onMilestoneEvent]
 * being invoked every step if the condition validation returns true.
 *
 * @since 0.4.0
 */
class StepMilestone
private constructor(
    private val builder: Builder
) : Milestone(builder) {

    private var called: Boolean = false

    override fun isOccurring(
        previousRouteProgress: RouteProgress,
        routeProgress: RouteProgress
    ): Boolean {
        // Determine if the step index has changed and set called accordingly. This prevents multiple calls to
        // onMilestoneEvent per Step.
        if (previousRouteProgress.currentLegProgress()?.stepIndex() != routeProgress.currentLegProgress()?.stepIndex()) {
            called = false
        }
        // If milestone's been called already on current step, no need to check triggers.
        if (called) {
            return false
        }
        return ifNonNull(builder.getTrigger()) { trigger ->
            val statementObjects = TriggerProperty.getSparseArray(previousRouteProgress, routeProgress)
            called = trigger.isOccurring(statementObjects)
            called
        } ?: false
    }

    /**
     * Build a new [StepMilestone]
     *
     * @since 0.4.0
     */
    class Builder : Milestone.Builder() {

        private var trigger: Trigger.Statement? = null

        override fun setTrigger(trigger: Trigger.Statement?): Builder {
            this.trigger = trigger
            return this
        }

        override fun getTrigger(): Trigger.Statement? = trigger

        override fun build(): StepMilestone {
            return StepMilestone(this)
        }
    }
}

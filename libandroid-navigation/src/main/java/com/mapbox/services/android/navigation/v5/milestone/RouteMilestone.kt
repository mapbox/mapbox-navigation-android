package com.mapbox.services.android.navigation.v5.milestone

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull

/**
 * Using a Route Milestone will result in
 * [MilestoneEventListener.onMilestoneEvent] being invoked only
 * once during a navigation session.
 *
 * @since 0.4.0
 */
class RouteMilestone
private constructor(
    private val builder: Builder
) : Milestone(builder) {

    private var called: Boolean = false

    override fun isOccurring(
        previousRouteProgress: RouteProgress,
        routeProgress: RouteProgress
    ): Boolean =
        ifNonNull(builder.getTrigger()) { trigger ->
            val statementObjects = TriggerProperty.getSparseArray(previousRouteProgress, routeProgress)
            called = trigger.isOccurring(statementObjects) && !called
            called
        } ?: false

    /**
     * Build a new [RouteMilestone]
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

        override fun build() = RouteMilestone(this)
    }
}

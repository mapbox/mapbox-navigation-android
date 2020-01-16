package com.mapbox.services.android.navigation.v5.milestone

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull

/**
 * A default milestone that is added to [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation]
 * when default milestones are enabled.
 *
 * Please note, this milestone has a custom trigger based on location progress along a route.  If you
 * set custom triggers, they will be ignored in favor of this logic.
 */
class BannerInstructionMilestone
private constructor(
    builder: Builder
) : Milestone(builder) {

    /**
     * Returns the given [BannerInstructions] for the time that the milestone is triggered.
     *
     * @return current banner instructions based on distance along the current step
     * @since 0.13.0
     */
    var bannerInstructions: BannerInstructions? = null
        private set

    override fun isOccurring(
        previousRouteProgress: RouteProgress,
        routeProgress: RouteProgress
    ): Boolean = updateCurrentBanner(routeProgress)

    private fun updateCurrentBanner(routeProgress: RouteProgress): Boolean =
        ifNonNull(
            routeProgress.bannerInstruction()
        ) {
            this.bannerInstructions = it
            true
        } ?: false

    class Builder : Milestone.Builder() {

        private var trigger: Trigger.Statement? = null

        override fun setTrigger(trigger: Trigger.Statement?): Builder {
            this.trigger = trigger
            return this
        }

        override fun getTrigger(): Trigger.Statement? = trigger

        override fun build() = BannerInstructionMilestone(this)
    }
}

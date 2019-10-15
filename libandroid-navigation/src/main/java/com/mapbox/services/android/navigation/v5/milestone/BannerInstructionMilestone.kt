package com.mapbox.services.android.navigation.v5.milestone

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.navigator.BannerSection
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import java.util.ArrayList

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
            routeProgress.bannerInstruction(),
            retrieveBannerFrom(routeProgress.bannerInstruction()?.primary)
        ) { currentBannerInstruction, primaryBannerText ->
            val secondaryBannerText: BannerText? = retrieveBannerFrom(currentBannerInstruction.secondary)
            val subBannerText: BannerText? = retrieveBannerFrom(currentBannerInstruction.sub)
            this.bannerInstructions = BannerInstructions.builder()
                .primary(primaryBannerText)
                .secondary(secondaryBannerText)
                .sub(subBannerText)
                .distanceAlongGeometry(currentBannerInstruction.remainingStepDistance.toDouble())
                .build()
            true
        } ?: false

    private fun retrieveBannerFrom(bannerSection: BannerSection?): BannerText? =
        bannerSection?.components?.let { currentComponents ->
            val primaryComponents = ArrayList<BannerComponents>()
            currentComponents.forEach {
                primaryComponents.add(
                    BannerComponents.builder()
                        .text(it.text)
                        .type(it.type)
                        .abbreviation(it.abbr)
                        .abbreviationPriority(it.abbrPriority)
                        .imageBaseUrl(it.imageBaseurl)
                        .directions(it.directions)
                        .active(it.active)
                        .build()
                )
            }
            BannerText.builder()
                .type(bannerSection.type)
                .modifier(bannerSection.modifier)
                .degrees(bannerSection.degrees?.toDouble())
                .drivingSide(bannerSection.drivingSide)
                .text(bannerSection.text)
                ?.components(primaryComponents)
                ?.build()
        }

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

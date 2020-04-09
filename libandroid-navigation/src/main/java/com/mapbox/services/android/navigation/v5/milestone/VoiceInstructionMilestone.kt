package com.mapbox.services.android.navigation.v5.milestone

import com.mapbox.services.android.navigation.v5.instruction.Instruction
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

/**
 * A default milestone that is added to [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation]
 * when default milestones are enabled.
 *
 * Please note, this milestone has a custom trigger based on location progress along a route.  If you
 * set custom triggers, they will be ignored in favor of this logic.
 */
class VoiceInstructionMilestone(builder: Builder) : Milestone(builder) {

    companion object {
        private const val EMPTY_STRING = ""
    }

    /**
     * Provide the instruction that can be used with Android's TextToSpeech.
     *
     * This string will be in plain text.
     *
     * @return announcement in plain text
     * @since 0.12.0
     */
    var announcement = EMPTY_STRING
        private set
    /**
     * Provide the SSML instruction that can be used with Mapbox's API Voice.
     *
     * This String will provide special markup denoting how certain portions of the announcement
     * should be pronounced.
     *
     * @return announcement with SSML markup
     * @since 0.8.0
     */
    var ssmlAnnouncement = EMPTY_STRING
        private set

    override val instruction: Instruction?
        get() = object : Instruction() {
            override fun buildInstruction(routeProgress: RouteProgress): String =
                announcement
        }

    override fun isOccurring(
        previousRouteProgress: RouteProgress,
        routeProgress: RouteProgress
    ): Boolean =
        updateCurrentAnnouncement(routeProgress)

    private fun updateCurrentAnnouncement(routeProgress: RouteProgress): Boolean =
        routeProgress.voiceInstruction()?.let {
            announcement = it.announcement() ?: EMPTY_STRING
            ssmlAnnouncement = it.ssmlAnnouncement() ?: EMPTY_STRING
            true
        } ?: false

    class Builder : Milestone.Builder() {

        private var trigger: Trigger.Statement? = null

        override fun setTrigger(trigger: Trigger.Statement?): Builder {
            this.trigger = trigger
            return this
        }

        override fun getTrigger(): Trigger.Statement? = trigger

        override fun build(): VoiceInstructionMilestone {
            return VoiceInstructionMilestone(this)
        }
    }
}

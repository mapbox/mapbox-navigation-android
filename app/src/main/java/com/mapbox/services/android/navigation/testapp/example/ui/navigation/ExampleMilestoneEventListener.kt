package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import androidx.lifecycle.MutableLiveData
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.voice.NavigationSpeechPlayer
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

class ExampleMilestoneEventListener(
    private val milestone: MutableLiveData<Milestone>,
    private val speechPlayer: NavigationSpeechPlayer
) : MilestoneEventListener {

    override fun onMilestoneEvent(
        routeProgress: RouteProgress,
        instruction: String,
        milestone: Milestone
    ) {
        this.milestone.value = milestone
        if (milestone is VoiceInstructionMilestone) {
            play(milestone)
        }
    }

    private fun play(milestone: VoiceInstructionMilestone) {
        val announcement = VoiceInstructions.builder()
            .announcement(milestone.announcement)
            .ssmlAnnouncement(milestone.ssmlAnnouncement)
            .build()
        speechPlayer.play(announcement)
    }
}

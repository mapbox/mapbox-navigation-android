package com.mapbox.services.android.navigation.testapp.example.ui.navigation

import android.arch.lifecycle.MutableLiveData
import com.mapbox.services.android.navigation.ui.v5.voice.NavigationSpeechPlayer
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

class ExampleMilestoneEventListener(private val milestone: MutableLiveData<Milestone>,
                                    private val speechPlayer: NavigationSpeechPlayer) : MilestoneEventListener {

  override fun onMilestoneEvent(routeProgress: RouteProgress, instruction: String, milestone: Milestone) {
    this.milestone.value = milestone
    if (milestone is VoiceInstructionMilestone) {
      play(milestone)
    }
  }

  private fun play(milestone: VoiceInstructionMilestone) {
    val announcement = SpeechAnnouncement.builder()
        .voiceInstructionMilestone(milestone)
        .build()
    speechPlayer.play(announcement)
  }
}
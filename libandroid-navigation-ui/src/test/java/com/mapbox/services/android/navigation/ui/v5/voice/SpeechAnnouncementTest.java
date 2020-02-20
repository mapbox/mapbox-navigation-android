package com.mapbox.services.android.navigation.ui.v5.voice;

import com.mapbox.services.android.navigation.v5.milestone.VoiceInstructionMilestone;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpeechAnnouncementTest {

  @Test
  public void milestoneAnnouncement_isUsedWhenProvided() {
    VoiceInstructionMilestone milestone = mock(VoiceInstructionMilestone.class);
    String announcement = "Milestone announcement";
    when(milestone.getAnnouncement()).thenReturn(announcement);

    SpeechAnnouncement speechAnnouncement = SpeechAnnouncement.builder()
      .voiceInstructionMilestone(milestone)
      .build();

    assertEquals(announcement, speechAnnouncement.announcement());
  }

  @Test
  public void milestoneSsmlAnnouncement_isUsedWhenProvided() {
    VoiceInstructionMilestone milestone = mock(VoiceInstructionMilestone.class);
    String announcement = "Milestone announcement";
    when(milestone.getAnnouncement()).thenReturn(announcement);
    String ssmlAnnouncement = "Milestone SSML announcement";
    when(milestone.getSsmlAnnouncement()).thenReturn(ssmlAnnouncement);

    SpeechAnnouncement speechAnnouncement = SpeechAnnouncement.builder()
      .voiceInstructionMilestone(milestone)
      .build();

    assertEquals(ssmlAnnouncement, speechAnnouncement.ssmlAnnouncement());
  }
}
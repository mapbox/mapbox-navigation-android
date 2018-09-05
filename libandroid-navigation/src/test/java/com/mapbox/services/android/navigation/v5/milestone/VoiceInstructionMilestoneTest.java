package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VoiceInstructionMilestoneTest {

  @Test
  public void sanity() {
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    assertNotNull(milestone);
  }

  @Test
  public void onSameInstructionOccurring_milestoneDoesNotTriggerTwice() {
    RouteProgress firstProgress = mock(RouteProgress.class);
    when(firstProgress.currentAnnouncement()).thenReturn("instruction");
    RouteProgress secondProgress = mock(RouteProgress.class);
    when(secondProgress.currentAnnouncement()).thenReturn("instruction");
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    milestone.isOccurring(firstProgress, firstProgress);
    boolean shouldNotBeOccurring = milestone.isOccurring(firstProgress, secondProgress);

    assertFalse(shouldNotBeOccurring);
  }

  @Test
  public void onOccurringMilestone_voiceSsmlInstructionsAreReturned() {
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.currentAnnouncement()).thenReturn("current announcement");
    String currentSsmlAnnouncement = "current SSML announcement";
    when(routeProgress.currentSsmlAnnouncement()).thenReturn(currentSsmlAnnouncement);
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    milestone.isOccurring(routeProgress, routeProgress);

    assertEquals(currentSsmlAnnouncement, milestone.getSsmlAnnouncement());
  }

  @Test
  public void onOccurringMilestone_voiceInstructionsAreReturned() throws Exception {
    RouteProgress routeProgress = mock(RouteProgress.class);
    String currentAnnouncement = "current announcement";
    when(routeProgress.currentAnnouncement()).thenReturn(currentAnnouncement);
    String currentSsmlAnnouncement = "current SSML announcement";
    when(routeProgress.currentSsmlAnnouncement()).thenReturn(currentSsmlAnnouncement);
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    milestone.isOccurring(routeProgress, routeProgress);

    assertEquals(currentAnnouncement, milestone.getAnnouncement());
  }

  @Test
  public void onOccurringMilestone_instructionsAreReturned() throws Exception {
    RouteProgress routeProgress = mock(RouteProgress.class);
    String currentAnnouncement = "current announcement";
    when(routeProgress.currentAnnouncement()).thenReturn(currentAnnouncement);
    String currentSsmlAnnouncement = "current SSML announcement";
    when(routeProgress.currentSsmlAnnouncement()).thenReturn(currentSsmlAnnouncement);
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    milestone.isOccurring(routeProgress, routeProgress);

    assertEquals(currentAnnouncement, milestone.getInstruction().buildInstruction(routeProgress));
  }

  @Test
  public void onNullMilestoneInstructions_emptyInstructionsAreReturned() throws Exception {
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    assertEquals("", milestone.getAnnouncement());
  }

  @Test
  public void onNullMilestoneInstructions_emptySsmlInstructionsAreReturned() throws Exception {
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    assertEquals("", milestone.getSsmlAnnouncement());
  }

  private VoiceInstructionMilestone buildVoiceInstructionMilestone() {
    return (VoiceInstructionMilestone) new VoiceInstructionMilestone.Builder().setIdentifier(1234).build();
  }
}

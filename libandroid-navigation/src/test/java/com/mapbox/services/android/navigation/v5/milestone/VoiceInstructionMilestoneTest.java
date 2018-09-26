package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.navigator.VoiceInstruction;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
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
    VoiceInstruction voiceInstruction = mock(VoiceInstruction.class);
    when(firstProgress.voiceInstruction()).thenReturn(voiceInstruction, null);
    when(voiceInstruction.getAnnouncement()).thenReturn("instruction");
    when(firstProgress.directionsRoute()).thenReturn(mock(DirectionsRoute.class));
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    milestone.isOccurring(firstProgress, firstProgress);
    boolean shouldNotBeOccurring = milestone.isOccurring(firstProgress, firstProgress);

    assertFalse(shouldNotBeOccurring);
  }

  @Test
  public void onOccurringMilestone_voiceSsmlInstructionsAreReturned() {
    RouteProgress routeProgress = mock(RouteProgress.class, RETURNS_DEEP_STUBS);
    when(routeProgress.directionsRoute()).thenReturn(mock(DirectionsRoute.class));
    when(routeProgress.voiceInstruction().getAnnouncement()).thenReturn("current announcement");
    String currentSsmlAnnouncement = "current SSML announcement";
    when(routeProgress.voiceInstruction().getSsmlAnnouncement()).thenReturn(currentSsmlAnnouncement);
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    milestone.isOccurring(routeProgress, routeProgress);

    assertEquals(currentSsmlAnnouncement, milestone.getSsmlAnnouncement());
  }

  @Test
  public void onOccurringMilestone_voiceInstructionsAreReturned() {
    RouteProgress routeProgress = mock(RouteProgress.class, RETURNS_DEEP_STUBS);
    when(routeProgress.directionsRoute()).thenReturn(mock(DirectionsRoute.class));
    String currentAnnouncement = "current announcement";
    when(routeProgress.voiceInstruction().getAnnouncement()).thenReturn(currentAnnouncement);
    String currentSsmlAnnouncement = "current SSML announcement";
    when(routeProgress.voiceInstruction().getSsmlAnnouncement()).thenReturn(currentSsmlAnnouncement);
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    milestone.isOccurring(routeProgress, routeProgress);

    assertEquals(currentAnnouncement, milestone.getAnnouncement());
  }

  @Test
  public void onOccurringMilestone_instructionsAreReturned() {
    RouteProgress routeProgress = mock(RouteProgress.class, RETURNS_DEEP_STUBS);
    when(routeProgress.directionsRoute()).thenReturn(mock(DirectionsRoute.class));
    String currentAnnouncement = "current announcement";
    when(routeProgress.voiceInstruction().getAnnouncement()).thenReturn(currentAnnouncement);
    String currentSsmlAnnouncement = "current SSML announcement";
    when(routeProgress.voiceInstruction().getSsmlAnnouncement()).thenReturn(currentSsmlAnnouncement);
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    milestone.isOccurring(routeProgress, routeProgress);

    assertEquals(currentAnnouncement, milestone.getInstruction().buildInstruction(routeProgress));
  }

  @Test
  public void onNullMilestoneInstructions_emptyInstructionsAreReturned() {
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    assertEquals("", milestone.getAnnouncement());
  }

  @Test
  public void onNullMilestoneInstructions_emptySsmlInstructionsAreReturned() {
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    assertEquals("", milestone.getSsmlAnnouncement());
  }

  private VoiceInstructionMilestone buildVoiceInstructionMilestone() {
    return (VoiceInstructionMilestone) new VoiceInstructionMilestone.Builder().setIdentifier(1234).build();
  }
}

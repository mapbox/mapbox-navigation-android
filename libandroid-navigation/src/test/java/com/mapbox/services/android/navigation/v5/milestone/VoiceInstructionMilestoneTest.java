package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class VoiceInstructionMilestoneTest extends BaseTest {

  @Test
  public void sanity() {
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    assertNotNull(milestone);
  }

  @Test
  public void onBeginningOfStep_voiceInstructionsShouldTrigger() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = createBeginningOfStepRouteProgress(routeProgress);
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    boolean isOccurring = milestone.isOccurring(routeProgress, routeProgress);

    assertTrue(isOccurring);
  }

  @Test
  public void onSameInstructionOccurring_milestoneDoesNotTriggerTwice() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    RouteProgress firstProgress = createBeginningOfStepRouteProgress(routeProgress);
    RouteProgress secondProgress = routeProgress.toBuilder()
      .stepDistanceRemaining(routeProgress.currentLegProgress().currentStep().distance() - 40)
      .stepIndex(0)
      .build();
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    milestone.isOccurring(firstProgress, firstProgress);
    boolean shouldNotBeOccurring = milestone.isOccurring(firstProgress, secondProgress);

    assertFalse(shouldNotBeOccurring);
  }

  @Test
  public void nullInstructions_doNotGetTriggered() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    List<VoiceInstructions> instructions = currentStep.voiceInstructions();
    instructions.clear();
    routeProgress = createBeginningOfStepRouteProgress(routeProgress);
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    boolean isOccurring = milestone.isOccurring(routeProgress, routeProgress);

    assertFalse(isOccurring);
  }

  @Test
  public void onOccurringMilestone_voiceSsmlInstructionsAreReturned() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = createBeginningOfStepRouteProgress(routeProgress);
    VoiceInstructions instructions = routeProgress.currentLegProgress().currentStep().voiceInstructions().get(0);
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    milestone.isOccurring(routeProgress, routeProgress);

    assertEquals(instructions.ssmlAnnouncement(), milestone.getSsmlAnnouncement());
  }

  @Test
  public void onOccurringMilestone_voiceInstructionsAreReturned() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = createBeginningOfStepRouteProgress(routeProgress);
    VoiceInstructions instructions = routeProgress.currentLegProgress().currentStep().voiceInstructions().get(0);
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    milestone.isOccurring(routeProgress, routeProgress);

    assertEquals(instructions.announcement(), milestone.getAnnouncement());
  }

  @Test
  public void onOccurringMilestone_instructionsAreReturned() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = createBeginningOfStepRouteProgress(routeProgress);
    VoiceInstructions instructions = routeProgress.currentLegProgress().currentStep().voiceInstructions().get(0);
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    milestone.isOccurring(routeProgress, routeProgress);

    assertEquals(instructions.announcement(), milestone.getInstruction().buildInstruction(routeProgress));
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

  @Test
  public void onNullMilestoneInstructions_stepNameIsReturnedForInstruction() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    VoiceInstructionMilestone milestone = buildVoiceInstructionMilestone();

    assertEquals(currentStep.name(), milestone.getInstruction().buildInstruction(routeProgress));
  }

  private RouteProgress createBeginningOfStepRouteProgress(RouteProgress routeProgress) {
    return routeProgress.toBuilder()
      .stepDistanceRemaining(routeProgress.currentLegProgress().currentStep().distance())
      .stepIndex(0)
      .build();
  }

  private VoiceInstructionMilestone buildVoiceInstructionMilestone() {
    return (VoiceInstructionMilestone) new VoiceInstructionMilestone.Builder().setIdentifier(1234).build();
  }
}

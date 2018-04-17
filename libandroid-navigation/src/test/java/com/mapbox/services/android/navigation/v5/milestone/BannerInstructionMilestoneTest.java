package com.mapbox.services.android.navigation.v5.milestone;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class BannerInstructionMilestoneTest extends BaseTest {

  @Test
  public void sanity() {
    BannerInstructionMilestone milestone = buildBannerInstructionMilestone();

    assertNotNull(milestone);
  }

  @Test
  public void onBeginningOfStep_bannerInstructionsShouldTrigger() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = createBeginningOfStepRouteProgress(routeProgress);
    BannerInstructionMilestone milestone = buildBannerInstructionMilestone();

    boolean isOccurring = milestone.isOccurring(routeProgress, routeProgress);

    assertTrue(isOccurring);
  }

  @Test
  public void onSameInstructionOccurring_milestoneDoesNotTriggerTwice() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    RouteProgress firstProgress = createBeginningOfStepRouteProgress(routeProgress);
    double fortyMetersIntoStep = routeProgress.currentLegProgress().currentStep().distance() - 40;
    RouteProgress secondProgress = routeProgress.toBuilder()
      .stepDistanceRemaining(fortyMetersIntoStep)
      .stepIndex(0)
      .build();
    BannerInstructionMilestone milestone = buildBannerInstructionMilestone();

    milestone.isOccurring(firstProgress, firstProgress);
    boolean shouldNotBeOccurring = milestone.isOccurring(firstProgress, secondProgress);

    assertFalse(shouldNotBeOccurring);
  }

  @Test
  public void nullInstructions_MilestoneDoesNotGetTriggered() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    List<BannerInstructions> instructions = currentStep.bannerInstructions();
    instructions.clear();
    routeProgress = createBeginningOfStepRouteProgress(routeProgress);
    BannerInstructionMilestone milestone = buildBannerInstructionMilestone();

    boolean isOccurring = milestone.isOccurring(routeProgress, routeProgress);

    assertFalse(isOccurring);
  }

  @Test
  public void onOccurringMilestone_beginningOfStep_bannerInstructionsAreReturned() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = routeProgress.toBuilder()
      .stepDistanceRemaining(routeProgress.currentLegProgress().currentStep().distance())
      .stepIndex(1)
      .build();
    BannerInstructions instructions = routeProgress.currentLegProgress().currentStep().bannerInstructions().get(0);
    BannerInstructionMilestone milestone = buildBannerInstructionMilestone();

    milestone.isOccurring(routeProgress, routeProgress);

    assertEquals(instructions, milestone.getBannerInstructions());
  }

  @Test
  public void onOccurringMilestone_endOfStep_bannerInstructionsAreReturned() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    int tenMetersRemainingInStep = 10;
    routeProgress = routeProgress.toBuilder()
      .stepDistanceRemaining(tenMetersRemainingInStep)
      .stepIndex(1)
      .build();
    List<BannerInstructions> bannerInstructions = routeProgress.currentLegProgress().currentStep().bannerInstructions();
    BannerInstructions instructions = bannerInstructions.get(bannerInstructions.size() - 1);
    BannerInstructionMilestone milestone = buildBannerInstructionMilestone();

    milestone.isOccurring(routeProgress, routeProgress);

    assertEquals(instructions, milestone.getBannerInstructions());
  }

  private RouteProgress createBeginningOfStepRouteProgress(RouteProgress routeProgress) {
    return routeProgress.toBuilder()
      .stepDistanceRemaining(routeProgress.currentLegProgress().currentStep().distance())
      .stepIndex(0)
      .build();
  }

  private BannerInstructionMilestone buildBannerInstructionMilestone() {
    return (BannerInstructionMilestone) new BannerInstructionMilestone.Builder().setIdentifier(1234).build();
  }
}

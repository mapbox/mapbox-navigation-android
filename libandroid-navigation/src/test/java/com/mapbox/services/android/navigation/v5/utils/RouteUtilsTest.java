package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class RouteUtilsTest extends BaseTest {

  @Test
  public void isNewRoute_returnsTrueWhenPreviousGeometriesNull() throws Exception {
    RouteProgress defaultRouteProgress = buildDefaultTestRouteProgress();
    boolean isNewRoute = RouteUtils.isNewRoute(null, defaultRouteProgress);
    assertTrue(isNewRoute);
    RouteProgress previousRouteProgress = buildDefaultTestRouteProgress();

    isNewRoute = RouteUtils.isNewRoute(previousRouteProgress, defaultRouteProgress);

    assertFalse(isNewRoute);
  }

  @Test
  public void isNewRoute_returnsFalseWhenGeometriesEqualEachOther() throws Exception {
    RouteProgress previousRouteProgress = buildDefaultTestRouteProgress();

    boolean isNewRoute = RouteUtils.isNewRoute(previousRouteProgress, previousRouteProgress);

    assertFalse(isNewRoute);
  }

  @Test
  public void isNewRoute_returnsTrueWhenGeometriesDoNotEqual() throws Exception {
    DirectionsRoute aRoute = buildTestDirectionsRoute();
    RouteProgress defaultRouteProgress = buildDefaultTestRouteProgress();
    RouteProgress previousRouteProgress = defaultRouteProgress.toBuilder()
      .directionsRoute(aRoute.toBuilder().geometry("vfejnqiv").build())
      .build();

    boolean isNewRoute = RouteUtils.isNewRoute(previousRouteProgress, defaultRouteProgress);

    assertTrue(isNewRoute);
  }

  @Test
  public void isArrivalEvent_returnsTrueWhenManeuverTypeIsArrival_andIsValidMetersRemaining() throws Exception {
    DirectionsRoute aRoute = buildTestDirectionsRoute();
    int lastStepIndex = obtainLastStepIndex(aRoute);
    RouteProgress defaultRouteProgress = buildDefaultTestRouteProgress();
    RouteProgress theRouteProgress = defaultRouteProgress.toBuilder()
      .stepDistanceRemaining(30)
      .legDistanceRemaining(30)
      .distanceRemaining(30)
      .stepIndex(lastStepIndex)
      .build();

    boolean isArrivalEvent = RouteUtils.isArrivalEvent(theRouteProgress);

    assertTrue(isArrivalEvent);
  }

  @Test
  public void isArrivalEvent_returnsFalseWhenManeuverTypeIsArrival_andIsNotValidMetersRemaining() throws Exception {
    DirectionsRoute aRoute = buildTestDirectionsRoute();
    int lastStepIndex = obtainLastStepIndex(aRoute);
    RouteProgress defaultRouteProgress = buildDefaultTestRouteProgress();
    RouteProgress theRouteProgress = defaultRouteProgress.toBuilder()
      .stepIndex(lastStepIndex)
      .legDistanceRemaining(100)
      .build();

    boolean isArrivalEvent = RouteUtils.isArrivalEvent(theRouteProgress);

    assertFalse(isArrivalEvent);
  }

  @Test
  public void isArrivalEvent_returnsTrueWhenUpcomingManeuverTypeIsArrival_andIsValidMetersRemaining() throws Exception {
    DirectionsRoute aRoute = buildTestDirectionsRoute();
    int lastStepIndex = obtainLastStepIndex(aRoute);
    RouteProgress defaultRouteProgress = buildDefaultTestRouteProgress();
    RouteProgress theRouteProgress = defaultRouteProgress.toBuilder()
      .legDistanceRemaining(30)
      .stepIndex(lastStepIndex - 1)
      .build();

    boolean isArrivalEvent = RouteUtils.isArrivalEvent(theRouteProgress);

    assertTrue(isArrivalEvent);
  }

  @Test
  public void isArrivalEvent_returnsFalseWhenManeuverTypeIsNotArrival_andIsNotValidMetersRemaining() throws Exception {
    DirectionsRoute aRoute = buildTestDirectionsRoute();
    int lastStepIndex = obtainLastStepIndex(aRoute);
    RouteProgress defaultRouteProgress = buildDefaultTestRouteProgress();
    RouteProgress theRouteProgress = defaultRouteProgress.toBuilder()
      .stepDistanceRemaining(200)
      .legDistanceRemaining(300)
      .distanceRemaining(300)
      .stepIndex(lastStepIndex - 1)
      .build();

    boolean isArrivalEvent = RouteUtils.isArrivalEvent(theRouteProgress);

    assertFalse(isArrivalEvent);
  }

  @Test
  public void isValidRouteProfile_returnsTrueWithDrivingTrafficProfile() throws Exception {
    String routeProfileDrivingTraffic = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC;

    boolean isValidProfile = RouteUtils.isValidRouteProfile(routeProfileDrivingTraffic);

    assertTrue(isValidProfile);
  }

  @Test
  public void isValidRouteProfile_returnsTrueWithDrivingProfile() throws Exception {
    String routeProfileDriving = DirectionsCriteria.PROFILE_DRIVING;

    boolean isValidProfile = RouteUtils.isValidRouteProfile(routeProfileDriving);

    assertTrue(isValidProfile);
  }

  @Test
  public void isValidRouteProfile_returnsTrueWithCyclingProfile() throws Exception {
    String routeProfileCycling = DirectionsCriteria.PROFILE_CYCLING;

    boolean isValidProfile = RouteUtils.isValidRouteProfile(routeProfileCycling);

    assertTrue(isValidProfile);
  }

  @Test
  public void isValidRouteProfile_returnsTrueWithWalkingProfile() throws Exception {
    String routeProfileWalking = DirectionsCriteria.PROFILE_WALKING;

    boolean isValidProfile = RouteUtils.isValidRouteProfile(routeProfileWalking);

    assertTrue(isValidProfile);
  }

  @Test
  public void isValidRouteProfile_returnsFalseWithInvalidProfile() throws Exception {
    String invalidProfile = "invalid_profile";

    boolean isValidProfile = RouteUtils.isValidRouteProfile(invalidProfile);

    assertFalse(isValidProfile);
  }

  @Test
  public void isValidRouteProfile_returnsFalseWithNullProfile() throws Exception {
    String nullProfile = null;

    boolean isValidProfile = RouteUtils.isValidRouteProfile(nullProfile);

    assertFalse(isValidProfile);
  }

  @Test
  public void findCurrentBannerInstructions_returnsNullWithNullCurrentStep() throws Exception {
    LegStep currentStep = null;
    double stepDistanceRemaining = 0;

    BannerInstructions currentBannerInstructions = RouteUtils.findCurrentBannerInstructions(
      currentStep, stepDistanceRemaining
    );

    assertNull(currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerInstructions_returnsNullWithCurrentStepEmptyInstructions() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
    List<BannerInstructions> currentInstructions = currentStep.bannerInstructions();
    currentInstructions.clear();

    BannerInstructions currentBannerInstructions = RouteUtils.findCurrentBannerInstructions(
      currentStep, stepDistanceRemaining
    );

    assertNull(currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerInstructions_clearsAllInvalidInstructions() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = 400;

    BannerInstructions currentBannerInstructions = RouteUtils.findCurrentBannerInstructions(
      currentStep, stepDistanceRemaining
    );

    assertNull(currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerInstructions_returnsCorrectCurrentInstruction() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();

    BannerInstructions currentBannerInstructions = RouteUtils.findCurrentBannerInstructions(
      currentStep, stepDistanceRemaining
    );

    assertEquals(currentStep.bannerInstructions().get(0), currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerInstructions_adjustedDistanceRemainingReturnsCorrectInstruction() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = routeProgress.toBuilder()
      .stepIndex(1)
      .stepDistanceRemaining(50)
      .build();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();

    BannerInstructions currentBannerInstructions = RouteUtils.findCurrentBannerInstructions(
      currentStep, stepDistanceRemaining
    );

    assertEquals(currentStep.bannerInstructions().get(1), currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerInstructions_adjustedDistanceRemainingRemovesCorrectInstructions() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = routeProgress.toBuilder()
      .stepIndex(1)
      .stepDistanceRemaining(500)
      .build();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();

    BannerInstructions currentBannerInstructions = RouteUtils.findCurrentBannerInstructions(
      currentStep, stepDistanceRemaining
    );

    assertEquals(currentStep.bannerInstructions().get(0), currentBannerInstructions);
  }

  @Test
  public void findCurrentBannerText_returnsCorrectPrimaryBannerText() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = routeProgress.toBuilder()
      .stepIndex(1)
      .stepDistanceRemaining(50)
      .build();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();

    BannerText currentBannerText = RouteUtils.findCurrentBannerText(
      currentStep, stepDistanceRemaining, true
    );

    assertEquals(currentStep.bannerInstructions().get(1).primary(), currentBannerText);
  }

  @Test
  public void findCurrentBannerText_returnsCorrectSecondaryBannerText() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = routeProgress.toBuilder()
      .stepIndex(1)
      .stepDistanceRemaining(50)
      .build();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();

    BannerText currentBannerText = RouteUtils.findCurrentBannerText(
      currentStep, stepDistanceRemaining, false
    );

    assertEquals(currentStep.bannerInstructions().get(1).secondary(), currentBannerText);
  }

  @Test
  public void findCurrentBannerText_returnsNullWithNullCurrentStep() throws Exception {
    LegStep currentStep = null;
    double stepDistanceRemaining = 0;

    BannerText currentBannerText = RouteUtils.findCurrentBannerText(
      currentStep, stepDistanceRemaining, false
    );

    assertNull(currentBannerText);
  }

  @Test
  public void findCurrentVoiceInstructions_returnsNullWithNullCurrentStep() throws Exception {
    LegStep currentStep = null;
    double stepDistanceRemaining = 0;

    VoiceInstructions currentVoiceInstructions = RouteUtils.findCurrentVoiceInstructions(
      currentStep, stepDistanceRemaining
    );

    assertNull(currentVoiceInstructions);
  }

  @Test
  public void findCurrentVoiceInstructions_returnsNullWithCurrentStepEmptyInstructions() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
    List<VoiceInstructions> currentInstructions = currentStep.voiceInstructions();
    currentInstructions.clear();

    VoiceInstructions voiceInstructions = RouteUtils.findCurrentVoiceInstructions(
      currentStep, stepDistanceRemaining
    );

    assertNull(voiceInstructions);
  }

  @Test
  public void findCurrentVoiceInstructions_returnsCorrectInstructionsBeginningOfStepDistanceRemaining() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = routeProgress.toBuilder()
      .stepIndex(1)
      .stepDistanceRemaining(400)
      .build();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();

    VoiceInstructions currentVoiceInstructions = RouteUtils.findCurrentVoiceInstructions(
      currentStep, stepDistanceRemaining
    );

    assertEquals(currentStep.voiceInstructions().get(1), currentVoiceInstructions);
  }

  @Test
  public void findCurrentVoiceInstructions_returnsCorrectInstructionsNoDistanceTraveled() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = routeProgress.toBuilder()
      .stepDistanceRemaining(routeProgress.currentLegProgress().currentStep().distance())
      .stepIndex(0)
      .build();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();

    VoiceInstructions currentVoiceInstructions = RouteUtils.findCurrentVoiceInstructions(
      currentStep, stepDistanceRemaining
    );

    assertEquals(currentStep.voiceInstructions().get(0), currentVoiceInstructions);
  }

  @Test
  public void findCurrentVoiceInstructions_returnsCorrectInstructionsEndOfStepDistanceRemaining() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    routeProgress = routeProgress.toBuilder()
      .stepIndex(1)
      .stepDistanceRemaining(50)
      .build();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    double stepDistanceRemaining = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();

    VoiceInstructions currentVoiceInstructions = RouteUtils.findCurrentVoiceInstructions(
      currentStep, stepDistanceRemaining
    );

    assertEquals(currentStep.voiceInstructions().get(2), currentVoiceInstructions);
  }

  private int obtainLastStepIndex(DirectionsRoute route) throws IOException {
    RouteLeg lastLeg = route.legs().get(route.legs().size() - 1);
    int lastStepIndex = lastLeg.steps().indexOf(lastLeg.steps().get(lastLeg.steps().size() - 1));
    return lastStepIndex;
  }
}
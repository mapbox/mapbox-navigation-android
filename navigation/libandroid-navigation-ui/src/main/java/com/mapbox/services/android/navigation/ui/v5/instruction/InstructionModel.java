package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.text.SpannableStringBuilder;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.navigation.v5.utils.abbreviation.StringAbbreviator;
import com.mapbox.services.api.directions.v5.models.IntersectionLanes;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.StepIntersection;
import com.mapbox.services.commons.utils.TextUtils;

import static com.mapbox.services.android.navigation.v5.utils.ManeuverUtils.getManeuverResource;

public class InstructionModel {

  private SpannableStringBuilder stepDistanceRemaining;
  private String textInstruction;
  private int maneuverImage;
  private String maneuverModifier;
  private IntersectionLanes[] turnLanes;

  public InstructionModel(RouteProgress progress) {
    buildCurrentRouteProgress(progress);
  }

  public SpannableStringBuilder getStepDistanceRemaining() {
    return stepDistanceRemaining;
  }

  public String getTextInstruction() {
    return textInstruction;
  }

  public int getManeuverImage() {
    return maneuverImage;
  }

  public IntersectionLanes[] getTurnLanes() {
    return turnLanes;
  }

  public String getManeuverModifier() {
    return maneuverModifier;
  }

  private void buildCurrentRouteProgress(RouteProgress progress) {
    this.stepDistanceRemaining = DistanceUtils.distanceFormatterBold(progress.currentLegProgress()
      .currentStepProgress().distanceRemaining());

    LegStep upComingStep = progress.currentLegProgress().upComingStep();
    if (upComingStep != null) {
      maneuverImage = getManeuverResource(upComingStep);
      if (hasManeuver(upComingStep)) {
        buildTextInstruction(upComingStep);
        maneuverModifier = upComingStep.getManeuver().getModifier();
      }
      if (hasIntersections(upComingStep)) {
        intersectionTurnLanes(upComingStep);
      }
    }
  }

  private boolean hasManeuver(LegStep upComingStep) {
    return upComingStep.getManeuver() != null;
  }

  private void intersectionTurnLanes(LegStep upComingStep) {
    StepIntersection intersection = upComingStep.getIntersections().get(0);
    turnLanes = intersection.getLanes();
  }

  private boolean hasIntersections(LegStep upComingStep) {
    return upComingStep.getIntersections() != null
      && upComingStep.getIntersections().get(0) != null;
  }

  private void buildTextInstruction(LegStep upComingStep) {
    if (hasDestinations(upComingStep)) {
      destinationInstruction(upComingStep);
    } else if (hasName(upComingStep)) {
      nameInstruction(upComingStep);
    } else if (hasManeuverInstruction(upComingStep)) {
      maneuverInstruction(upComingStep);
    }
  }

  private void maneuverInstruction(LegStep upComingStep) {
    textInstruction = upComingStep.getManeuver().getInstruction();
  }

  private boolean hasManeuverInstruction(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.getManeuver().getInstruction());
  }

  private void nameInstruction(LegStep upComingStep) {
    textInstruction = upComingStep.getName();
  }

  private boolean hasName(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.getName());
  }

  private void destinationInstruction(LegStep upComingStep) {
    textInstruction = upComingStep.getDestinations().trim();
    textInstruction = StringAbbreviator.deliminator(textInstruction);
  }

  private boolean hasDestinations(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.getDestinations());
  }
}

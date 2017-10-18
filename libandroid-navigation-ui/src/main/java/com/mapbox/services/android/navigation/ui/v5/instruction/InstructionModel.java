package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.text.SpannableStringBuilder;

import com.mapbox.directions.v5.models.IntersectionLanes;
import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.directions.v5.models.StepIntersection;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.navigation.v5.utils.abbreviation.StringAbbreviator;
import com.mapbox.services.commons.utils.TextUtils;

import java.text.DecimalFormat;
import java.util.List;

import static com.mapbox.services.android.navigation.v5.utils.ManeuverUtils.getManeuverResource;

public class InstructionModel {

  private SpannableStringBuilder stepDistanceRemaining;
  private String textInstruction;
  private int maneuverImage;
  private String maneuverModifier;
  private List<IntersectionLanes> turnLanes;

  public InstructionModel(RouteProgress progress, DecimalFormat decimalFormat) {
    buildInstructionModel(progress, decimalFormat);
  }

  SpannableStringBuilder getStepDistanceRemaining() {
    return stepDistanceRemaining;
  }

  String getTextInstruction() {
    return textInstruction;
  }

  int getManeuverImage() {
    return maneuverImage;
  }

  List<IntersectionLanes> getTurnLanes() {
    return turnLanes;
  }

  String getManeuverModifier() {
    return maneuverModifier;
  }

  private void buildInstructionModel(RouteProgress progress, DecimalFormat decimalFormat) {
    formatStepDistance(progress, decimalFormat);
    LegStep upComingStep = progress.currentLegProgress().upComingStep();
    if (upComingStep != null) {
      extractStepResources(upComingStep);
    }
  }

  private void extractStepResources(LegStep upComingStep) {
    maneuverImage = getManeuverResource(upComingStep);
    if (hasManeuver(upComingStep)) {
      buildTextInstruction(upComingStep);
      maneuverModifier = upComingStep.maneuver().modifier();
    }
    if (hasIntersections(upComingStep)) {
      intersectionTurnLanes(upComingStep);
    }
  }

  private void formatStepDistance(RouteProgress progress, DecimalFormat decimalFormat) {
    stepDistanceRemaining = DistanceUtils.distanceFormatterBold(progress.currentLegProgress()
      .currentStepProgress().distanceRemaining(), decimalFormat);
  }

  private boolean hasManeuver(LegStep upComingStep) {
    return upComingStep.maneuver() != null;
  }

  private void intersectionTurnLanes(LegStep upComingStep) {
    StepIntersection intersection = upComingStep.intersections().get(0);
    List<IntersectionLanes> lanes = intersection.lanes();
    if (checkForNoneIndications(lanes)) {
      turnLanes = null;
      return;
    }
    turnLanes = lanes;
  }

  private boolean checkForNoneIndications(List<IntersectionLanes> lanes) {
    if (lanes == null) {
      return true;
    }
    for (IntersectionLanes lane : lanes) {
      for (String indication : lane.indications()) {
        if (indication.contains("none")) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasIntersections(LegStep upComingStep) {
    return upComingStep.intersections() != null
      && upComingStep.intersections().get(0) != null;
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
    textInstruction = upComingStep.maneuver().instruction();
  }

  private boolean hasManeuverInstruction(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.maneuver().instruction());
  }

  private void nameInstruction(LegStep upComingStep) {
    textInstruction = upComingStep.name();
  }

  private boolean hasName(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.name());
  }

  private void destinationInstruction(LegStep upComingStep) {
    textInstruction = upComingStep.destinations().trim();
    textInstruction = StringAbbreviator.deliminator(textInstruction);
  }

  private boolean hasDestinations(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.destinations());
  }
}

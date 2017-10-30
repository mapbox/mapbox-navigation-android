package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.text.SpannableStringBuilder;

import com.mapbox.directions.v5.models.IntersectionLanes;
import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.directions.v5.models.StepIntersection;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;

import java.text.DecimalFormat;
import java.util.List;

import static com.mapbox.services.android.navigation.v5.utils.ManeuverUtils.getManeuverResource;

public class InstructionModel {

  private SpannableStringBuilder stepDistanceRemaining;
  private TextInstruction instruction;
  private int maneuverImage;
  private String maneuverModifier;
  private List<IntersectionLanes> turnLanes;
  private RouteProgress progress;

  public InstructionModel(RouteProgress progress, DecimalFormat decimalFormat) {
    this.progress = progress;
    buildInstructionModel(progress, decimalFormat);
  }

  SpannableStringBuilder getStepDistanceRemaining() {
    return stepDistanceRemaining;
  }

  String getPrimaryText() {
    if (instruction != null) {
      return instruction.getPrimaryText();
    } else {
      return "";
    }
  }

  String getSecondaryText() {
    if (instruction != null) {
      return instruction.getSecondaryText();
    } else {
      return "";
    }
  }

  int getManeuverImage() {
    return maneuverImage;
  }

  String getManeuverModifier() {
    return maneuverModifier;
  }

  List<IntersectionLanes> getTurnLanes() {
    return turnLanes;
  }

  RouteProgress getProgress() {
    return progress;
  }

  private void buildInstructionModel(RouteProgress progress, DecimalFormat decimalFormat) {
    formatStepDistance(progress, decimalFormat);
    LegStep upComingStep = progress.currentLegProgress().upComingStep();
    if (upComingStep != null) {
      extractStepResources(upComingStep);
    }
  }

  private void extractStepResources(LegStep upComingStep) {
    if (hasManeuver(upComingStep)) {
      maneuverModifier = upComingStep.maneuver().modifier();
    }
    if (hasIntersections(upComingStep)) {
      intersectionTurnLanes(upComingStep);
    }
    instruction = new TextInstruction(upComingStep);
    maneuverImage = getManeuverResource(upComingStep);
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
}

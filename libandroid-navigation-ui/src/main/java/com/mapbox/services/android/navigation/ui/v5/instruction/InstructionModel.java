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
import java.util.Arrays;
import java.util.List;

import static com.mapbox.services.android.navigation.v5.utils.ManeuverUtils.getManeuverResource;

public class InstructionModel {

  private SpannableStringBuilder stepDistanceRemaining;
  private String primaryText;
  private String secondaryText;
  private int maneuverImage;
  private String maneuverModifier;
  private List<IntersectionLanes> turnLanes;
  private boolean isUsingInstruction;

  public InstructionModel(RouteProgress progress, DecimalFormat decimalFormat) {
    buildInstructionModel(progress, decimalFormat);
  }

  SpannableStringBuilder getStepDistanceRemaining() {
    return stepDistanceRemaining;
  }

  String getPrimaryText() {
    return primaryText;
  }

  String getSecondaryText() {
    return secondaryText;
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

  boolean isUsingInstruction() {
    return isUsingInstruction;
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
      maneuverModifier = upComingStep.maneuver().modifier();
    }
    if (hasIntersections(upComingStep)) {
      intersectionTurnLanes(upComingStep);
    }
    buildTextInstructions(upComingStep);
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

  private void buildTextInstructions(LegStep upComingStep) {
    String exitText = "";

    // Extract Exit for later use
    if (upComingStep.maneuver() != null) {
      if (!TextUtils.isEmpty(upComingStep.exits())) {
        exitText = "Exit " + upComingStep.exits();
      }
    }

    // Refs
    if (hasRefs(upComingStep)) {
      primaryText = StringAbbreviator.deliminator(upComingStep.ref());
      if (hasDestination(upComingStep)) {
        secondaryText = destinations(upComingStep);
      }
      return;
    }

    // Multiple Destinations
    if (hasMultipleDestinations(upComingStep)) {
      formatMultipleStrings(upComingStep.destinations(), exitText);
      return;
    }

    // Multiple Names
    if (hasMultipleNames(upComingStep)) {
      formatMultipleStrings(upComingStep.name(), exitText);
      return;
    }

    // Destination or Street Name
    if (hasDestination(upComingStep)) {
      primaryText = destinations(upComingStep);
      return;
    } else if (hasName(upComingStep)) {
      primaryText = names(upComingStep);
      return;
    }

    // Instruction
    if (hasInstruction(upComingStep)) {
      primaryText = instruction(upComingStep);
      isUsingInstruction = true;
    }
  }

  private boolean hasRefs(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.ref());
  }

  private String instruction(LegStep upComingStep) {
    return upComingStep.maneuver().instruction();
  }

  private boolean hasInstruction(LegStep upComingStep) {
    return upComingStep.maneuver() != null
      && !TextUtils.isEmpty(upComingStep.maneuver().instruction());
  }

  private String names(LegStep upComingStep) {
    String instruction = upComingStep.name().trim();
    return StringAbbreviator.deliminator(instruction);
  }

  private boolean hasName(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.name());
  }

  private String destinations(LegStep upComingStep) {
    String instruction = upComingStep.destinations().trim();
    return StringAbbreviator.deliminator(instruction);
  }

  private boolean hasDestination(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.destinations());
  }

  private boolean hasMultipleDestinations(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.destinations())
      && StringAbbreviator.splitter(upComingStep.destinations()).length > 1;
  }

  private boolean hasMultipleNames(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.name())
      && StringAbbreviator.splitter(upComingStep.name()).length > 1;
  }

  private void formatMultipleStrings(String multipleString, String exitText) {
    String[] strings = StringAbbreviator.splitter(multipleString);
    String[] firstString = Arrays.copyOfRange(strings, 0, 1);
    if (!TextUtils.isEmpty(exitText)) {
      primaryText = exitText + " " + firstString[0];
    } else {
      primaryText = firstString[0];
    }
    String[] remainingStrings = Arrays.copyOfRange(strings, 1, strings.length);
    secondaryText = TextUtils.join(" / ", remainingStrings).trim();
  }
}

package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.v5.utils.abbreviation.StringAbbreviator;
import com.mapbox.services.utils.TextUtils;

import java.util.Arrays;

public class TextInstruction {

  private String primaryText;
  private String secondaryText;
  private LegStep legStep;
  private double stepDistance;

  public TextInstruction(LegStep legStep) {
    this.legStep = legStep;
    stepDistance = legStep.distance();
    buildTextInstructions(legStep);
  }

  public String getPrimaryText() {
    return StringAbbreviator.abbreviate(primaryText);
  }

  public String getSecondaryText() {
    return StringAbbreviator.abbreviate(secondaryText);
  }

  public double getStepDistance() {
    return stepDistance;
  }

  public LegStep getStep() {
    return legStep;
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
        secondaryText = destination(upComingStep);
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
      primaryText = destination(upComingStep);
      return;
    } else if (hasName(upComingStep)) {
      primaryText = name(upComingStep);
      return;
    }

    // Instruction
    if (hasInstruction(upComingStep)) {
      primaryText = instruction(upComingStep);
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

  private String name(LegStep upComingStep) {
    return upComingStep.name().trim();
  }

  private boolean hasName(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.name());
  }

  private String destination(LegStep upComingStep) {
    return upComingStep.destinations().trim();
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
      primaryText = exitText + ": " + firstString[0];
    } else {
      primaryText = firstString[0];
    }
    String[] remainingStrings = Arrays.copyOfRange(strings, 1, strings.length);
    secondaryText = TextUtils.join("  / ", remainingStrings).trim();
  }
}

package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.services.android.navigation.v5.utils.abbreviation.StringAbbreviator;
import com.mapbox.services.utils.TextUtils;

import java.util.Arrays;

public class InstructionText {

  private TextFields stepTextFields;
  private double stepDistance;

  public InstructionText(LegStep step) {
    stepDistance = step.distance();
    stepTextFields = buildInstructionText(step);
  }

  public String getPrimaryText() {
    return StringAbbreviator.abbreviate(stepTextFields.primaryText);
  }

  public String getSecondaryText() {
    return StringAbbreviator.abbreviate(stepTextFields.secondaryText);
  }

  public double getStepDistance() {
    return stepDistance;
  }

  private TextFields buildInstructionText(LegStep step) {

    TextFields textFields = new TextFields();
    String exitText = "";

    // Extract Exit for later use
    if (step.maneuver() != null) {
      if (!TextUtils.isEmpty(step.exits())) {
        exitText = "Exit " + step.exits();
      }
    }

    // Refs
    if (hasRefs(step) && isMotorway(step)) {
      textFields.primaryText = StringAbbreviator.deliminator(step.ref());
      if (hasDestination(step)) {
        textFields.secondaryText = destination(step);
      }
      return textFields;
    }

    // Multiple Destinations
    if (hasMultipleDestinations(step)) {
      return formatMultipleStrings(step.destinations(), exitText);
    }

    // Multiple Names
    if (hasMultipleNames(step)) {
      return formatMultipleStrings(step.name(), exitText);
    }

    // Destination or Street Name
    if (hasDestination(step)) {
      textFields.primaryText = destination(step);
      return textFields;
    } else if (hasName(step)) {
      textFields.primaryText = name(step);
      return textFields;
    }

    // Fall back to instruction
    if (hasInstruction(step)) {
      textFields.primaryText = instruction(step);
      return textFields;
    }
    return textFields;
  }

  private boolean hasRefs(LegStep upComingStep) {
    return !TextUtils.isEmpty(upComingStep.ref());
  }

  private boolean isMotorway(LegStep step) {
    if (step.intersections() == null || step.intersections().isEmpty()) {
      return false;
    }
    if (step.intersections().get(0).classes() == null || step.intersections().get(0).classes().isEmpty()) {
      return false;
    }
    return step.intersections().get(0).classes().contains("motorway");
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

  private TextFields formatMultipleStrings(String multipleString, String exitText) {
    TextFields textFields = new TextFields();

    String[] strings = {multipleString};

    if (!(multipleString.contains("(") || multipleString.contains(")"))) {
      strings = StringAbbreviator.splitter(multipleString);
    }

    String[] firstString = Arrays.copyOfRange(strings, 0, 1);
    if (!TextUtils.isEmpty(exitText)) {
      textFields.primaryText = exitText + ": " + firstString[0];
    } else {
      textFields.primaryText = firstString[0];
    }

    if (strings.length > 1) {
      String[] remainingStrings = Arrays.copyOfRange(strings, 1, strings.length);
      textFields.secondaryText = TextUtils.join("  / ", remainingStrings).trim();
    }

    return textFields;
  }

  private class TextFields {
    String primaryText = "";
    String secondaryText = "";
  }
}

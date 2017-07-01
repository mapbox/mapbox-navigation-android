package com.mapbox.services.android.navigation.v5.instruction;

import android.text.TextUtils;
import android.util.SparseArray;

import com.mapbox.services.android.navigation.v5.NavigationConstants;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfHelpers;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Creates voice instructions based on the default milestones
 * provided by {@link com.mapbox.services.android.navigation.v5.MapboxNavigation}
 *
 * @since 0.4.0
 */
class DefaultInstructionEngine extends SparseArray<DefaultInstructionEngine.InstructionBuilder> {

  private static final double MINIMUM_UPCOMING_STEP_DISTANCE = 15d;
  private static final String DECIMAL_FORMAT = "###.#";
  private static final String MILES_STRING_FORMAT = "%s miles";
  private static final String FEET_STRING_FORMAT = "%s feet";
  private static final String IN_STRING_FORMAT = "In %s %s";
  private static final String THEN_STRING_FORMAT = "%s then %s";
  private static final String THEN_IN_STRING_FORMAT = "%s then in %s %s";
  private static final String CONTINUE_STRING_FORMAT = "Continue on %s for %s";

  DefaultInstructionEngine() {
    super(5);
    initDefaultBuilders();
  }

  private void initDefaultBuilders() {
    this.put(NavigationConstants.DEPARTURE_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        return buildDepartureInstruction(routeProgress);
      }
    });
    this.put(NavigationConstants.NEW_STEP_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        return buildNewStepInstruction(routeProgress);
      }
    });
    this.put(NavigationConstants.IMMINENT_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        return buildImminentInstruction(routeProgress);
      }
    });
    this.put(NavigationConstants.URGENT_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        return buildUrgentInstruction(routeProgress);
      }
    });
    this.put(NavigationConstants.ARRIVAL_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        return buildArrivalInstruction(routeProgress);
      }
    });
  }

  /**
   * If the next step is greater than 15 meters long, use continue format instruction.
   * Otherwise, use then in format for instruction
   *
   * @param progress {@link RouteProgress} created by the location change
   * @return {@link String} to be announced on departure milestone
   * @since 0.4.0
   */
  private String buildDepartureInstruction(RouteProgress progress) {
    if (progress.getCurrentLegProgress().getUpComingStep().getDistance() > MINIMUM_UPCOMING_STEP_DISTANCE) {
      return buildContinueFormatInstruction(progress);
    } else {
      return buildThenInFormatInstruction(progress);
    }
  }

  /**
   * Create default string format instruction for new step milestone
   *
   * @param progress {@link RouteProgress} created by the location change
   * @return {@link String} to be announced on new step milestone
   * @since 0.4.0
   */
  private String buildNewStepInstruction(RouteProgress progress) {
    return buildDefaultFormatInstruction(progress);
  }

  /**
   * @param progress {@link RouteProgress} created by the location change
   * @return {@link String} to be announced on imminent milestone
   */
  private String buildImminentInstruction(RouteProgress progress) {
    return buildContinueFormatInstruction(progress);
  }

  /**
   * If the next step is less than 15 meters long, use then string format instruction.
   * Otherwise, just use the upcoming step instruction
   *
   * @param progress {@link RouteProgress} created by the location change
   * @return {@link String} to be announced on urgent milestone
   * @since 0.4.0
   */
  private String buildUrgentInstruction(RouteProgress progress) {
    if (progress.getCurrentLegProgress().getUpComingStep().getDistance() < MINIMUM_UPCOMING_STEP_DISTANCE) {
      return buildThenFormatInstruction(progress);
    } else {
      return progress.getCurrentLegProgress().getUpComingStep().getManeuver().getInstruction();
    }
  }

  /**
   * On arrival, use the upcoming step instruction.
   * If empty, use the current step instruction as a fallback
   *
   * @param progress {@link RouteProgress} created by the location change
   * @return {@link String} to be announced on departure milestone
   * @since 0.4.0
   */
  private String buildArrivalInstruction(RouteProgress progress) {
    if (progress.getCurrentLegProgress().getUpComingStep() != null) {
      String instruction = progress.getCurrentLegProgress().getUpComingStep().getManeuver().getInstruction();
      if (!TextUtils.isEmpty(instruction)) {
        return instruction;
      }
    } else {
      return progress.getCurrentLegProgress().getCurrentStep().getManeuver().getInstruction();
    }
    return "";
  }

  /**
   * Creates a {@link String} with the current step name and distance remaining
   * Example: "Continue on Main St. for 3.2 miles"
   *
   * @param progress {@link RouteProgress} created by the location change
   * @return {@link String} with format "Continue on %s for %s"
   * @since 0.4.0
   */
  private String buildContinueFormatInstruction(RouteProgress progress) {
    double userDistance = progress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    if (TextUtils.isEmpty(progress.getCurrentLegProgress().getCurrentStep().getName()) || userDistance == 0) {
      return "";
    } else {
      return String.format(
        Locale.US,
        CONTINUE_STRING_FORMAT,
        progress.getCurrentLegProgress().getCurrentStep().getName(),
        distanceFormatter(userDistance));
    }
  }

  /**
   * Creates a {@link String} with the current step distance remaining upcoming step instruction
   * Example: "In 3.2 miles turn left onto Main St."
   *
   * @param progress {@link RouteProgress} created by the location change
   * @return {@link String} with format "In %s %s"
   * @since 0.4.0
   */
  private String buildDefaultFormatInstruction(RouteProgress progress) {
    double userDistance = progress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    return String.format(
      Locale.US,
      IN_STRING_FORMAT,
      distanceFormatter(userDistance),
      convertFirstCharLowercase(progress.getCurrentLegProgress()
        .getUpComingStep().getManeuver().getInstruction())
    );
  }

  /**
   * Creates a {@link String} with the current step maneuver instruction, current step distance remaining,
   * and upcoming step instruction
   * Example: "Turn left onto Main St. then in 3.2 miles turn right onto Second St."
   *
   * @param progress {@link RouteProgress} created by the location change
   * @return {@link String} with format "%s then in %s %s"
   * @since 0.4.0
   */
  private String buildThenInFormatInstruction(RouteProgress progress) {
    double userDistance = progress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    String currentStepInstruction = progress.getCurrentLegProgress().getCurrentStep()
      .getManeuver().getInstruction();
    return String.format(
      Locale.US,
      THEN_IN_STRING_FORMAT,
      currentStepInstruction,
      distanceFormatter(userDistance),
      convertFirstCharLowercase(progress.getCurrentLegProgress()
        .getUpComingStep().getManeuver().getInstruction())
    );
  }

  /**
   * Creates a {@link String} with the upcoming step instruction and follow up step instruction
   * Example: "Turn right onto Main St. then turn left onto Second St."
   *
   * @param progress {@link RouteProgress} created by the location change
   * @return {@link String} with format "%s then %s"
   * @since 0.4.0
   */
  private String buildThenFormatInstruction(RouteProgress progress) {
    int legIndex = progress.getLegIndex();
    int followUpStepIndex = progress.getCurrentLegProgress().getStepIndex() + 2;
    return String.format(
      Locale.US,
      THEN_STRING_FORMAT,
      progress.getCurrentLegProgress().getUpComingStep().getManeuver().getInstruction(),
      convertFirstCharLowercase(progress.getRoute().getLegs().get(legIndex)
        .getSteps().get(followUpStepIndex).getManeuver().getInstruction())
    );
  }

  private static String convertFirstCharLowercase(String instruction) {
    if (TextUtils.isEmpty(instruction)) {
      return instruction;
    } else {
      return instruction.substring(0, 1).toLowerCase() + instruction.substring(1);
    }
  }

  /**
   * If over 1099 feet, use miles format.  If less, use feet in intervals of 100
   *
   * @param distance given distance extracted from {@link RouteProgress}
   * @return {@link String} in either feet (int) or miles (decimal) format
   * @since 0.4.0
   */
  private static String distanceFormatter(double distance) {
    String formattedString;
    if (TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_FEET) > 1099) {
      distance = TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_MILES);
      DecimalFormat df = new DecimalFormat(DECIMAL_FORMAT);
      double roundedNumber = (distance / 100 * 100);
      formattedString = String.format(Locale.US, MILES_STRING_FORMAT, df.format(roundedNumber));
    } else {
      distance = TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_FEET);
      int roundedNumber = ((int) Math.round(distance)) / 100 * 100;
      formattedString = String.format(Locale.US, FEET_STRING_FORMAT, roundedNumber);
    }
    return formattedString;
  }

  interface InstructionBuilder {
    String build(RouteProgress routeProgress);
  }
}

package com.mapbox.services.android.navigation.v5.instruction;

import android.text.TextUtils;
import android.util.SparseArray;

import com.mapbox.services.android.navigation.v5.NavigationConstants;
import com.mapbox.services.android.navigation.v5.RouteProgress;
import com.mapbox.services.api.directions.v5.models.StepManeuver;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfHelpers;

import java.text.DecimalFormat;
import java.util.Locale;

class DefaultInstructionEngine extends SparseArray<DefaultInstructionEngine.InstructionBuilder> {

  private static final double MINIMUM_UPCOMING_STEP_DISTANCE = 15d;
  private static final String DECIMAL_FORMAT = "###.#";
  private static final String MILES_STRING_FORMAT = "%s Miles";
  private static final String FEET_STRING_FORMAT = "%s Feet";
  private static final String IN_STRING_FORMAT = "In %s %s";
  private static final String THEN_STRING_FORMAT = "%s then %s";
  private static final String THEN_IN_STRING_FORMAT = "%s then in %s %s";
  private static final String CONTINUE_STRING_FORMAT = "Continue on %s for %s";

  DefaultInstructionEngine() {
    initDefaultBuilders();
  }

  private void initDefaultBuilders() {
    this.put(NavigationConstants.DEPARTURE_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        buildDepartureInstruction(routeProgress);
        return null;
      }
    });
    this.put(NavigationConstants.NEW_STEP_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        buildNewStepInstruction(routeProgress);
        return null;
      }
    });
    this.put(NavigationConstants.IMMINENT_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        buildImminentInstruction(routeProgress);
        return null;
      }
    });
    this.put(NavigationConstants.URGENT_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        buildUrgentInstruction(routeProgress);
        return null;
      }
    });
    this.put(NavigationConstants.ARRIVAL_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        return buildArrivalInstruction(routeProgress);
      }
    });
  }

  private String buildDepartureInstruction(RouteProgress progress) {
    if (progress.getCurrentLegProgress().getUpComingStep().getDistance() > MINIMUM_UPCOMING_STEP_DISTANCE) {
      return buildContinueFormatInstruction(progress);
    } else {
      return buildThenInFormatInstruction(progress);
    }
  }

  private String buildNewStepInstruction(RouteProgress progress) {
    return buildDefaultFormatInstruction(progress);
  }

  private String buildImminentInstruction(RouteProgress progress) {
    return buildContinueFormatInstruction(progress);
  }

  private String buildUrgentInstruction(RouteProgress progress) {
    String urgentInstruction = buildThenFormatInstruction(progress);

    if (!TextUtils.isEmpty(urgentInstruction)) {
      return urgentInstruction;
    } else {
      return buildDefaultFormatInstruction(progress);
    }
  }

  private String buildArrivalInstruction(RouteProgress progress) {
    String instruction = progress.getCurrentLegProgress().getUpComingStep().getManeuver().getInstruction();
    if (!TextUtils.isEmpty(instruction)) {
      return instruction;
    } else {
      return buildDefaultFormatInstruction(progress);
    }
  }

  private String buildContinueFormatInstruction(RouteProgress progress) {
    double userDistance = progress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    return String.format(
      Locale.US,
      CONTINUE_STRING_FORMAT,
      progress.getCurrentLegProgress().getCurrentStep().getName(),
      distanceFormatter(userDistance));
  }

  private String buildDefaultFormatInstruction(RouteProgress progress) {
    double userDistance = progress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    return String.format(
      Locale.US,
      IN_STRING_FORMAT,
      distanceFormatter(userDistance),
      progress.getCurrentLegProgress().getCurrentStep().getManeuver().getInstruction()
    );
  }

  private String buildThenInFormatInstruction(RouteProgress progress) {
    double userDistance = progress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    StepManeuver currentStepManeuver = progress.getCurrentLegProgress().getCurrentStep().getManeuver();
    return String.format(
      Locale.US,
      THEN_IN_STRING_FORMAT,
      currentStepManeuver.getInstruction(),
      distanceFormatter(userDistance),
      progress.getCurrentLegProgress().getUpComingStep().getManeuver().getInstruction()
    );
  }

  private String buildThenFormatInstruction(RouteProgress progress) {
    int legIndex = progress.getLegIndex();
    int followUpStepIndex = progress.getCurrentLegProgress().getStepIndex() + 2;

    return String.format(
      Locale.US,
      THEN_STRING_FORMAT,
      progress.getCurrentLegProgress().getUpComingStep().getManeuver().getInstruction(),
      progress.getRoute().getLegs().get(legIndex).getSteps().get(followUpStepIndex).getManeuver().getInstruction()
    );
  }

  private static String distanceFormatter(double distance) {
    String formattedString;
    if (TurfHelpers.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_FEET) > 1999) {
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

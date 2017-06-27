package com.mapbox.services.android.navigation.v5.instruction;

import android.text.TextUtils;

import com.mapbox.services.android.navigation.v5.NavigationConstants;
import com.mapbox.services.android.navigation.v5.RouteProgress;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;

import timber.log.Timber;

class InstructionEngine extends HashMap<Integer, InstructionEngine.InstructionBuilder> {

  private static final String DECIMAL_FORMAT = "###.#";
  private static final String MILES_STRING_FORMAT = "%s Miles";
  private static final String FEET_STRING_FORMAT = "%s Feet";
  private static final String IN_STRING_FORMAT = "In %s %s";

  InstructionEngine() {
    this.put(NavigationConstants.DEPARTURE_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        getDepartureInstruction();
        return null;
      }
    });
    this.put(NavigationConstants.NEW_STEP_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        getNewStepInstruction();
        return null;
      }
    });
    this.put(NavigationConstants.IMMINENT_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        getImminentInstruction();
        return null;
      }
    });
    this.put(NavigationConstants.URGENT_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        getUrgentInstruction();
        return null;
      }
    });
    this.put(NavigationConstants.ARRIVAL_MILESTONE, new InstructionBuilder() {
      @Override
      public String build(RouteProgress routeProgress) {
        getArrivalInstruction(routeProgress);
        return null;
      }
    });
  }

  private void getDepartureInstruction() {
    Timber.d("Departing");
  }

  private void getNewStepInstruction() {
    Timber.d("New Step");
  }

  private void getImminentInstruction() {
    Timber.d("Imminent");
  }

  private void getUrgentInstruction() {
    Timber.d("Urgent");
  }

  private String getArrivalInstruction(RouteProgress progress) {
    String instruction = progress.getCurrentLegProgress().getUpComingStep().getManeuver().getInstruction();
    if (!TextUtils.isEmpty(instruction)) {
      return instruction;
    } else {
      return getDefaultInstruction(progress);
    }
  }

  private String getDefaultInstruction(RouteProgress progress) {
    double userDistance = progress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
    return String.format(
      Locale.US,
      IN_STRING_FORMAT,
      distanceFormatter(userDistance),
      progress.getCurrentLegProgress().getCurrentStep().getManeuver().getInstruction()
    );
  }

  private static String distanceFormatter(double distance) {
    String formattedString;
    if (LengthUnit.convert(LengthUnit.Unit.METERS, LengthUnit.Unit.FEET, distance) > 1999) {
      distance = LengthUnit.convert(LengthUnit.Unit.METERS, LengthUnit.Unit.MILES, distance);
      DecimalFormat df = new DecimalFormat(DECIMAL_FORMAT);
      double roundedNumber = (distance / 100 * 100);
      formattedString = String.format(Locale.US, MILES_STRING_FORMAT, df.format(roundedNumber));
    } else {
      distance = LengthUnit.convert(LengthUnit.Unit.METERS, LengthUnit.Unit.FEET, distance);
      int roundedNumber = ((int) Math.round(distance)) / 100 * 100;
      formattedString = String.format(Locale.US, FEET_STRING_FORMAT, roundedNumber);
    }
    return formattedString;
  }

  interface InstructionBuilder {
    String build(RouteProgress routeProgress);
  }
}

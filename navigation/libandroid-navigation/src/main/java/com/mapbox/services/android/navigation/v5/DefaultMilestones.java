package com.mapbox.services.android.navigation.v5;

import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.milestone.RouteMilestone;
import com.mapbox.services.android.navigation.v5.milestone.StepMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Trigger;
import com.mapbox.services.android.navigation.v5.milestone.TriggerProperty;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfHelpers;
import com.mapbox.services.commons.utils.TextUtils;

import java.text.DecimalFormat;
import java.util.Locale;

class DefaultMilestones {

  private static final String DECIMAL_FORMAT = "###.#";
  private static final String MILES_STRING_FORMAT = "%s miles";
  private static final String FEET_STRING_FORMAT = "%s feet";

  private MapboxNavigation navigation;

  DefaultMilestones(MapboxNavigation navigation) {
    this.navigation = navigation;
    initialize();
  }

  private void initialize() {

    /*
     * Urgent milestones
     */

    navigation.addMilestone(new StepMilestone.Builder()
      .setIdentifier(NavigationConstants.URGENT_MILESTONE)
      .setInstruction(new Instruction() {
        @Override
        public String buildInstruction(RouteProgress routeProgress) {
          return routeProgress.getCurrentLegProgress().getUpComingStep().getManeuver().getInstruction();
        }
      })
      .setTrigger(
        Trigger.any(
          Trigger.all(
            Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 30d),
            Trigger.lt(TriggerProperty.STEP_DURATION_REMAINING_SECONDS, 10d),
            Trigger.neq(TriggerProperty.FIRST_STEP, TriggerProperty.TRUE),
            Trigger.gt(TriggerProperty.NEXT_STEP_DISTANCE_METERS, 15d)
          ),
          Trigger.lte(TriggerProperty.STEP_DISTANCE_REMAINING_METERS, 10d)
        )
      )
      .build()
    );

    navigation.addMilestone(new StepMilestone.Builder()
      .setIdentifier(NavigationConstants.URGENT_MILESTONE)
      .setInstruction(new Instruction() {
        @Override
        public String buildInstruction(RouteProgress routeProgress) {
          int legIndex = routeProgress.getLegIndex();
          int followUpStepIndex = routeProgress.getCurrentLegProgress().getStepIndex() + 2;
          return String.format(Locale.US, "%s then %s",
            routeProgress.getCurrentLegProgress().getUpComingStep().getManeuver().getInstruction(),
            convertFirstCharLowercase(routeProgress.getRoute().getLegs().get(legIndex)
              .getSteps().get(followUpStepIndex).getManeuver().getInstruction())
          );
        }
      })
      .setTrigger(
        Trigger.any(
          Trigger.all(
            Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 30d),
            Trigger.lt(TriggerProperty.STEP_DURATION_REMAINING_SECONDS, 10d),
            Trigger.neq(TriggerProperty.FIRST_STEP, TriggerProperty.TRUE),
            Trigger.neq(TriggerProperty.LAST_STEP, TriggerProperty.TRUE),
            Trigger.lte(TriggerProperty.NEXT_STEP_DISTANCE_METERS, 15d)
          ),
          Trigger.lte(TriggerProperty.STEP_DISTANCE_REMAINING_METERS, 10d)
        )
      )
      .build()
    );

    /*
     * Imminent milestone
     */

    navigation.addMilestone(new StepMilestone.Builder()
      .setIdentifier(NavigationConstants.IMMINENT_MILESTONE)
      .setInstruction(new Instruction() {
        @Override
        public String buildInstruction(RouteProgress routeProgress) {
          double userDistance = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
          if (TextUtils.isEmpty(routeProgress.getCurrentLegProgress().getCurrentStep().getName())
            || userDistance == 0) {
            return "";
          } else {
            return String.format(Locale.US, "Continue on %s for %s",
              routeProgress.getCurrentLegProgress().getCurrentStep().getName(), distanceFormatter(userDistance));
          }
        }
      })
      .setTrigger(
        Trigger.all(
          Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 400d),
          Trigger.gt(TriggerProperty.STEP_DURATION_TOTAL_SECONDS, 80d),
          Trigger.lt(TriggerProperty.STEP_DURATION_REMAINING_SECONDS, 70d)
        )
      )
      .build()
    );

    /*
     * New step milestones
     */

    navigation.addMilestone(new StepMilestone.Builder()
      .setIdentifier(NavigationConstants.NEW_STEP_MILESTONE)
      .setInstruction(new Instruction() {
        @Override
        public String buildInstruction(RouteProgress routeProgress) {
          double userDistance = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
          return String.format(Locale.US, "In %s %s", distanceFormatter(userDistance),
            convertFirstCharLowercase(routeProgress.getCurrentLegProgress()
              .getUpComingStep().getManeuver().getInstruction())
          );
        }
      })
      .setTrigger(
        Trigger.all(
          Trigger.neq(TriggerProperty.NEW_STEP, TriggerProperty.FALSE),
          Trigger.neq(TriggerProperty.FIRST_STEP, TriggerProperty.TRUE),
          Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 100d)
        )
      ).build());

    /*
     * Departure milestones
     */

    navigation.addMilestone(new RouteMilestone.Builder()
      .setIdentifier(NavigationConstants.DEPARTURE_MILESTONE)
      .setInstruction(new Instruction() {
        @Override
        public String buildInstruction(RouteProgress routeProgress) {
          double userDistance = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
          if (TextUtils.isEmpty(routeProgress.getCurrentLegProgress().getCurrentStep().getName())
            || userDistance == 0) {
            return "";
          } else {
            return String.format(Locale.US, "Continue on %s for %s and than %s",
              routeProgress.getCurrentLegProgress().getCurrentStep().getName(), distanceFormatter(userDistance),
              routeProgress.getCurrentLegProgress().getUpComingStep().getManeuver().getInstruction());
          }
        }
      })
      .setTrigger(
        Trigger.all(
          Trigger.eq(TriggerProperty.FIRST_STEP, TriggerProperty.TRUE),
          Trigger.eq(TriggerProperty.FIRST_LEG, TriggerProperty.TRUE),
          Trigger.gt(TriggerProperty.NEXT_STEP_DISTANCE_METERS, 15)
        )
      ).build());

    navigation.addMilestone(new RouteMilestone.Builder()
      .setIdentifier(NavigationConstants.DEPARTURE_MILESTONE)
      .setInstruction(new Instruction() {
        @Override
        public String buildInstruction(RouteProgress routeProgress) {
          double userDistance = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getDistanceRemaining();
          String currentStepInstruction = routeProgress.getCurrentLegProgress().getCurrentStep()
            .getManeuver().getInstruction();
          return String.format(Locale.US, "%s then in %s %s", currentStepInstruction, distanceFormatter(userDistance),
            convertFirstCharLowercase(routeProgress.getCurrentLegProgress()
              .getUpComingStep().getManeuver().getInstruction())
          );
        }
      })
      .setTrigger(
        Trigger.all(
          Trigger.eq(TriggerProperty.FIRST_STEP, TriggerProperty.TRUE),
          Trigger.eq(TriggerProperty.FIRST_LEG, TriggerProperty.TRUE),
          Trigger.lte(TriggerProperty.NEXT_STEP_DISTANCE_METERS, 15)
        )
      ).build());

    /*
     * Arrival milestones
     */

    navigation.addMilestone(new RouteMilestone.Builder()
      .setIdentifier(NavigationConstants.ARRIVAL_MILESTONE)
      .setInstruction(new Instruction() {
        @Override
        public String buildInstruction(RouteProgress routeProgress) {
          return routeProgress.getCurrentLegProgress().getCurrentStep().getManeuver().getInstruction();
        }
      })
      .setTrigger(
        Trigger.all(
          Trigger.eq(TriggerProperty.LAST_STEP, TriggerProperty.TRUE),
          Trigger.eq(TriggerProperty.LAST_LEG, TriggerProperty.TRUE),
          Trigger.lte(TriggerProperty.STEP_DISTANCE_REMAINING_METERS, 25)
        )
      ).build());
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
}
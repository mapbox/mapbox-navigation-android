package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.milestone.RouteMilestone;
import com.mapbox.services.android.navigation.v5.milestone.StepMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Trigger;
import com.mapbox.services.android.navigation.v5.milestone.TriggerProperty;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.commons.utils.TextUtils;

import java.util.Locale;

import static com.mapbox.services.android.navigation.v5.utils.StringUtils.convertFirstCharLowercase;
import static com.mapbox.services.android.navigation.v5.utils.StringUtils.distanceFormatter;

class DefaultMilestones {

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
          return getInstructionString(routeProgress);
        }
      })
      .setTrigger(
        Trigger.all(
          Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 15d),
          Trigger.lte(TriggerProperty.STEP_DISTANCE_REMAINING_METERS, 15d),
          Trigger.gt(TriggerProperty.NEXT_STEP_DISTANCE_METERS, 15d)
        )
      )
      .build()
    );

    navigation.addMilestone(new StepMilestone.Builder()
      .setIdentifier(NavigationConstants.URGENT_MILESTONE)
      .setInstruction(new Instruction() {
        @Override
        public String buildInstruction(RouteProgress routeProgress) {
          int legIndex = routeProgress.legIndex();
          int followUpStepIndex = routeProgress.currentLegProgress().stepIndex() + 2;
          return String.format(Locale.US, "%s then %s",
            getInstructionString(routeProgress),
            convertFirstCharLowercase(routeProgress.directionsRoute().getLegs().get(legIndex)
              .getSteps().get(followUpStepIndex).getManeuver().getInstruction())
          );// TODO fix this
        }
      })
      .setTrigger(
        Trigger.all(
          Trigger.gt(TriggerProperty.STEP_DISTANCE_TOTAL_METERS, 15d),
          Trigger.lte(TriggerProperty.STEP_DISTANCE_REMAINING_METERS, 15d),
          Trigger.lte(TriggerProperty.NEXT_STEP_DISTANCE_METERS, 15d),
          Trigger.neq(TriggerProperty.LAST_STEP, TriggerProperty.TRUE)// TODO fix this to support multi legs routes
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
          double userDistance = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
          if (TextUtils.isEmpty(routeProgress.currentLegProgress().currentStep().getName())
            || userDistance == 0) {
            return "";
          } else {
            return String.format(Locale.US, "In %s, %s", distanceFormatter(userDistance),
              convertFirstCharLowercase(getInstructionString(routeProgress)));
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
          double userDistance = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
          return String.format(Locale.US, "Continue on %s for %s",
            routeProgress.currentLegProgress().currentStep().getName(), distanceFormatter(userDistance)
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
          double userDistance = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
          if (TextUtils.isEmpty(routeProgress.currentLegProgress().currentStep().getName())
            || userDistance == 0) {
            return "";
          } else {
            return String.format(Locale.US, "Continue on %s for %s and then %s",
              routeProgress.currentLegProgress().currentStep().getName(), distanceFormatter(userDistance),
              getInstructionString(routeProgress));
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
          double userDistance = routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
          return String.format(Locale.US, "%s then in %s %s", getInstructionString(routeProgress),
            distanceFormatter(userDistance),
            convertFirstCharLowercase(getInstructionString(routeProgress))
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
          return getInstructionString(routeProgress);
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

  private String getInstructionString(RouteProgress routeProgress) {
    return routeProgress.currentLegProgress().upComingStep() != null
      ? routeProgress.currentLegProgress().upComingStep().getManeuver().getInstruction() :
      routeProgress.currentLegProgress().currentStep().getManeuver().getInstruction();
  }
}
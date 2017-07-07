package com.mapbox.services.android.navigation.v5.instruction.defaultinstructions;

import com.mapbox.services.android.navigation.v5.NavigationConstants;
import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

/**
 * Used to provide the {@link String} instruction in
 * {@link MilestoneEventListener#onMilestoneEvent(RouteProgress, String, int)}
 * for New Step Milestones
 *
 * @since 0.4.0
 */
public class NewStepInstruction extends Instruction {

  @Override
  public String buildInstruction(RouteProgress routeProgress) {
    return DefaultInstructionEngine.createInstruction(routeProgress, NavigationConstants.NEW_STEP_MILESTONE);
  }
}

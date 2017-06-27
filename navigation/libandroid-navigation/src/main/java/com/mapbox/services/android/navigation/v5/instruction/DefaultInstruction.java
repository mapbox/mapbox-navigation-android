package com.mapbox.services.android.navigation.v5.instruction;

import com.mapbox.services.android.navigation.v5.RouteProgress;

public class DefaultInstruction extends Instruction {

  private DefaultInstructionEngine defaultInstructionEngine;
  private String instruction;

  public DefaultInstruction(RouteProgress routeProgress, int identifier) {
    super(routeProgress, identifier);
    defaultInstructionEngine = new DefaultInstructionEngine();
    instruction = createInstruction(routeProgress, identifier);
  }

  @Override
  public String getInstruction() {
    return instruction;
  }

  private String createInstruction(RouteProgress routeProgress, int identifier) {
    return defaultInstructionEngine.get(identifier).build(routeProgress);
  }
}

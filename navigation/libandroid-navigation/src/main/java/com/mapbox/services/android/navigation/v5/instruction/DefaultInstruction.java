package com.mapbox.services.android.navigation.v5.instruction;

import com.mapbox.services.android.navigation.v5.RouteProgress;

public class DefaultInstruction extends Instruction {

  private InstructionEngine instructionEngine;
  private String instruction;

  public DefaultInstruction(RouteProgress routeProgress, int identifier) {
    super(routeProgress);
    instructionEngine = new InstructionEngine();
    instruction = createInstruction(routeProgress, identifier);
  }

  @Override
  public String getInstruction() {
    return instruction;
  }

  private String createInstruction(RouteProgress routeProgress, int identifier) {
    return instructionEngine.get(identifier).build(routeProgress);
  }
}

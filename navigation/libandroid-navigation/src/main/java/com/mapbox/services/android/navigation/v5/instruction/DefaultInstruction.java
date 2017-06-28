package com.mapbox.services.android.navigation.v5.instruction;

import com.mapbox.services.android.navigation.v5.RouteProgress;

public class DefaultInstruction extends Instruction {

  private static final String EMPTY_STRING = "";

  private DefaultInstructionEngine defaultInstructionEngine;
  private String instruction;

  /**
   * Uses the {@link DefaultInstructionEngine} to create an instruction based on the
   * passed {@link RouteProgress} and milestone identifier
   * @param routeProgress for current route data / distance
   * @param identifier for what type of instruction we want to build
   * @since 0.4.0
   */
  public DefaultInstruction(RouteProgress routeProgress, int identifier) {
    defaultInstructionEngine = new DefaultInstructionEngine();
    instruction = createInstruction(routeProgress, identifier);
  }

  @Override
  public String getInstruction() {
    return instruction;
  }

  /**
   * Provides the {@link RouteProgress} and milestone identifier to the {@link DefaultInstructionEngine}
   * which returns the appropriate instruction.  Will return an empty {@link String} if the
   * milestone identifier provided is not one of the default identifiers
   * @param routeProgress for current route data / distance
   * @param identifier for what type of instruction we want to build
   * @return {@link String} instruction that has been created by the engine
   * @since 0.4.0
   */
  private String createInstruction(RouteProgress routeProgress, int identifier) {
    if (defaultInstructionEngine.get(identifier) != null) {
      return defaultInstructionEngine.get(identifier).build(routeProgress);
    } else {
      return EMPTY_STRING;
    }
  }
}

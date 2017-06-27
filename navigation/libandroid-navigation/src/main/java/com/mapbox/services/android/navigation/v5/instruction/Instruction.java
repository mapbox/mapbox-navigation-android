package com.mapbox.services.android.navigation.v5.instruction;

import com.mapbox.services.android.navigation.v5.RouteProgress;

public abstract class Instruction {

  Instruction(RouteProgress routeProgress, int identifier) {
  }

  public abstract String getInstruction();
}

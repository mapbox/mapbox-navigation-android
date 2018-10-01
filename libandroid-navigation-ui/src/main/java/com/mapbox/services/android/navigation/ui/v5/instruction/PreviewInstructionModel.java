package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

public class PreviewInstructionModel {
  private RouteProgress routeProgress;
  private Integer currentPreviewStepIndex;

  public PreviewInstructionModel(RouteProgress routeProgress) {
    this.routeProgress = routeProgress;
  }

  public LegStep currentStep() {
    if (currentPreviewStepIndex == null) {
      return null;
    }
    List<LegStep> steps = routeProgress.remainingLegSteps();
    return steps.get(currentPreviewStepIndex);
  }

  public RouteLeg currentLeg() {
    LegStep currentStep = currentStep();
    if (currentStep == null) {
      return null;
    }
    return PreviewInstructionUtils.legContaining(routeProgress, currentStep());
  }

  public int stepIndex() {
    LegStep currentStep = currentStep();
    if (currentStep == null) {
      return -1;
    }
    return currentLeg().steps().indexOf(currentStep());
  }

  public int legIndex() {
    RouteLeg currentLeg = currentLeg();
    if (currentLeg == null) {
      return -1;
    }
    return routeProgress.directionsRoute().legs().indexOf(currentLeg);
  }

  public LegStep nextStep() {
    LegStep nextStep = PreviewInstructionUtils.nextBannerStep(routeProgress.remainingLegSteps(),
        currentPreviewStepIndex);

    if (nextStep != null) {
      currentPreviewStepIndex = currentPreviewStepIndex == null ? 1 : currentPreviewStepIndex + 1;
      return nextStep;
    }

    return null;
  }

  public LegStep previousStep() {
    if (currentPreviewStepIndex == null || routeProgress == null) {
      return null;
    }

    LegStep prevStep = PreviewInstructionUtils.previousBannerStep(routeProgress.remainingLegSteps(),
        currentPreviewStepIndex);
    if (prevStep != null) {
      currentPreviewStepIndex = currentPreviewStepIndex - 1;
      return prevStep;
    }

    return null;
  }

  public List<List<Point>> getPolyline() {
    return PreviewInstructionUtils.getPolyline(routeProgress.directionsRoute(), legIndex(), stepIndex());
  }

  public void update(RouteProgress routeProgress) {
    this.routeProgress = routeProgress;
  }

  public void reset() {
    currentPreviewStepIndex = null;
  }
}

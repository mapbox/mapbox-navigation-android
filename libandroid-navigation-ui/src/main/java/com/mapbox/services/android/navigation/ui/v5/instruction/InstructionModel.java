package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.text.SpannableString;

import com.mapbox.api.directions.v5.models.IntersectionLanes;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepIntersection;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;

import java.util.List;

public class InstructionModel {

  private RouteProgress progress;
  private SpannableString stepDistanceRemaining;
  private List<IntersectionLanes> turnLanes;
  private String upcomingManeuverModifier;

  public InstructionModel(DistanceFormatter distanceFormatter, RouteProgress progress) {
    this.progress = progress;
    double distanceRemaining = progress.currentLegProgress().currentStepProgress().distanceRemaining();
    stepDistanceRemaining = distanceFormatter.formatDistance(distanceRemaining);
    extractStepResources(progress);
  }

  RouteProgress retrieveProgress() {
    return progress;
  }

  SpannableString retrieveStepDistanceRemaining() {
    return stepDistanceRemaining;
  }

  String retrieveUpcomingManeuverModifier() {
    return upcomingManeuverModifier;
  }

  List<IntersectionLanes> retrieveTurnLanes() {
    return turnLanes;
  }

  private void extractStepResources(RouteProgress progress) {
    LegStep upcomingStep = progress.currentLegProgress().upComingStep();
    if (upcomingStep != null) {
      if (hasIntersections(upcomingStep)) {
        intersectionTurnLanes(upcomingStep);
      }
      upcomingManeuverModifier = upcomingStep.maneuver().modifier();
    }
  }

  private void intersectionTurnLanes(LegStep step) {
    StepIntersection intersection = step.intersections().get(0);
    List<IntersectionLanes> lanes = intersection.lanes();
    if (checkForNoneIndications(lanes)) {
      turnLanes = null;
      return;
    }
    turnLanes = lanes;
  }

  private boolean checkForNoneIndications(List<IntersectionLanes> lanes) {
    if (lanes == null) {
      return true;
    }
    for (IntersectionLanes lane : lanes) {
      for (String indication : lane.indications()) {
        if (indication.contains("none")) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasIntersections(LegStep step) {
    return step.intersections() != null
      && step.intersections().get(0) != null;
  }
}

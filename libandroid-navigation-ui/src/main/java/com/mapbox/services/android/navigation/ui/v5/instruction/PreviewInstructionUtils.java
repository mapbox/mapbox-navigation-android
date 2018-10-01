package com.mapbox.services.android.navigation.ui.v5.instruction;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.navigation.NavigationHelper;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

public class PreviewInstructionUtils {
  public static RouteLeg legContaining(RouteProgress routeProgress, LegStep step) {
    for (RouteLeg leg : routeProgress.directionsRoute().legs()) {
      if (leg.steps().contains(step)) {
        return leg;
      }
    }
    return null;
  }

  public static LegStep previousBannerStep(List<LegStep> remainingLegSteps, Integer currentBannerStepIndex) {
    int prevStepIndex = currentBannerStepIndex - 1;
    if (prevStepIndex < 0) {
      return null;
    }
    return remainingLegSteps.get(prevStepIndex);
  }

  public static LegStep nextBannerStep(List<LegStep> remainingLegSteps, Integer currentBannerStepIndex) {
    int curStepIndex = currentBannerStepIndex != null ? currentBannerStepIndex : 0;
    int nextStepIndex = curStepIndex + 1;
    if (nextStepIndex >= remainingLegSteps.size()) {
      return null;
    }
    return remainingLegSteps.get(nextStepIndex);
  }

  public static LegStep currentStep(RouteLeg leg, int stepIndex) {
    return leg.steps().get(stepIndex);
  }

  public static LegStep upcomingStep(RouteLeg leg, int currentStepIndex) {
    if (leg.steps().size() - 1 > currentStepIndex) {
      return leg.steps().get(currentStepIndex + 1);
    }
    return null;
  }

  public static LegStep followOnStep(RouteLeg leg, int currentStepIndex) {
    if (leg.steps().size() - 2 > currentStepIndex) {
      return leg.steps().get(currentStepIndex + 2);
    }
    return null;
  }

  public static List<List<Point>> getPolyline(DirectionsRoute route, int legIndex, int stepIndex) {
    List<List<Point>> polyline = new ArrayList<>();

    List<Point> precedingPoints = NavigationHelper.decodeStepPoints(route, new ArrayList<Point>(), legIndex, stepIndex);
    polyline.add(precedingPoints);

    List<Point> followingPoints = NavigationHelper.decodeStepPoints(route, new ArrayList<Point>(),
        legIndex, stepIndex + 1);
    polyline.add(followingPoints);

    return polyline;
  }
}

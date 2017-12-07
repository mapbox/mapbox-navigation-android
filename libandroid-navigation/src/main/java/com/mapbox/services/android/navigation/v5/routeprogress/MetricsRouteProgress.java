package com.mapbox.services.android.navigation.v5.routeprogress;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.StepManeuver;
import com.mapbox.geojson.Point;

public class MetricsRouteProgress {

  private int directionsRouteDistance;
  private int directionsRouteDuration;
  private String directionsRouteProfile;
  private Point directionsRouteDestination;
  private int distanceRemaining;
  private int durationRemaining;
  private int distanceTraveled;
  private int currentStepDistance;
  private int currentStepDuration;
  private int currentStepDistanceRemaining;
  private int currentStepDurationRemaining;
  private String currentStepName;

  private String upcomingStepInstruction;
  private String upcomingStepModifier;
  private String upcomingStepType;
  private String upcomingStepName;

  private String previousStepInstruction;
  private String previousStepModifier;
  private String previousStepType;
  private String previousStepName;

  public MetricsRouteProgress(@Nullable RouteProgress routeProgress) {
    if (routeProgress != null) {
      obtainRouteData(routeProgress.directionsRoute());
      obtainLegData(routeProgress.currentLegProgress());
      obtainStepData(routeProgress);
      this.distanceRemaining = (int) routeProgress.distanceRemaining();
      this.durationRemaining = (int) routeProgress.durationRemaining();
      this.distanceTraveled = (int) routeProgress.distanceTraveled();
    } else {
      initDefaultValues();
    }
  }

  private void initDefaultValues() {
    directionsRouteProfile = "";
    directionsRouteDestination = Point.fromLngLat(0d, 0d);
    currentStepName = "";
    upcomingStepInstruction = "";
    upcomingStepModifier = "";
    upcomingStepType = "";
    upcomingStepName = "";
    previousStepInstruction = "";
    previousStepModifier = "";
    previousStepType = "";
    previousStepName = "";
  }

  private void obtainRouteData(DirectionsRoute route) {
    directionsRouteDistance = route.distance() != null ? route.distance().intValue() : 0;
    directionsRouteDuration = route.duration() != null ? route.duration().intValue() : 0;
    directionsRouteProfile = hasRouteProfile(route) ? route.routeOptions().profile() : "";
    directionsRouteDestination = retrieveRouteDestination(route);
  }

  private void obtainLegData(RouteLegProgress legProgress) {
    currentStepDistance = (int) legProgress.currentStep().distance();
    currentStepDuration = (int) legProgress.currentStep().duration();
    currentStepDistanceRemaining = (int) legProgress.currentStepProgress().distanceRemaining();
    currentStepDurationRemaining = (int) legProgress.currentStepProgress().durationRemaining();
    currentStepName = hasStepName(legProgress) ? legProgress.currentStep().name() : "";
  }

  private void obtainStepData(RouteProgress routeProgress) {
    RouteLegProgress legProgress = routeProgress.currentLegProgress();
    if (legProgress.upComingStep() != null) {
      upcomingStepName = legProgress.upComingStep().name();
      StepManeuver upcomingManeuver = legProgress.upComingStep().maneuver();
      if (upcomingManeuver != null) {
        upcomingStepInstruction = upcomingManeuver.instruction();
        upcomingStepType = upcomingManeuver.type();
        upcomingStepModifier = upcomingManeuver.modifier();
      }
    }
    StepManeuver currentManeuver = legProgress.currentStep().maneuver();
    if (currentManeuver != null) {
      previousStepInstruction = currentManeuver.instruction();
      previousStepType = currentManeuver.type();
      previousStepModifier = currentManeuver.modifier();
    }
    previousStepName = currentStepName;
  }

  private boolean hasRouteProfile(DirectionsRoute route) {
    return route.routeOptions() != null && !TextUtils.isEmpty(route.routeOptions().profile());
  }

  private Point retrieveRouteDestination(DirectionsRoute route) {
    RouteLeg lastLeg = route.legs().get(route.legs().size() - 1);
    LegStep lastStep = lastLeg.steps().get(lastLeg.steps().size() - 1);
    StepManeuver finalManuever = lastStep.maneuver();
    if (finalManuever.location() != null) {
      return finalManuever.location();
    }
    return Point.fromLngLat(0d, 0d);
  }

  private boolean hasStepName(RouteLegProgress legProgress) {
    return legProgress.currentStep().name() != null && !TextUtils.isEmpty(legProgress.currentStep().name());
  }

  public int getDirectionsRouteDistance() {
    return directionsRouteDistance;
  }

  public int getDirectionsRouteDuration() {
    return directionsRouteDuration;
  }

  public String getDirectionsRouteProfile() {
    return directionsRouteProfile;
  }

  public Point getDirectionsRouteDestination() {
    return directionsRouteDestination;
  }

  public int getDistanceRemaining() {
    return distanceRemaining;
  }

  public int getDurationRemaining() {
    return durationRemaining;
  }

  public int getDistanceTraveled() {
    return distanceTraveled;
  }

  public int getCurrentStepDistance() {
    return currentStepDistance;
  }

  public int getCurrentStepDuration() {
    return currentStepDuration;
  }

  public int getCurrentStepDistanceRemaining() {
    return currentStepDistanceRemaining;
  }

  public int getCurrentStepDurationRemaining() {
    return currentStepDurationRemaining;
  }

  public String getUpcomingStepInstruction() {
    return upcomingStepInstruction;
  }

  public String getUpcomingStepModifier() {
    return upcomingStepModifier;
  }

  public String getUpcomingStepType() {
    return upcomingStepType;
  }

  public String getUpcomingStepName() {
    return upcomingStepName;
  }

  public String getPreviousStepInstruction() {
    return previousStepInstruction;
  }

  public String getPreviousStepModifier() {
    return previousStepModifier;
  }

  public String getPreviousStepType() {
    return previousStepType;
  }

  public String getPreviousStepName() {
    return previousStepName;
  }
}

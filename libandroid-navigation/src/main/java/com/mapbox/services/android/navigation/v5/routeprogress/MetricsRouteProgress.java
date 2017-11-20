package com.mapbox.services.android.navigation.v5.routeprogress;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.LegStep;
import com.mapbox.directions.v5.models.RouteLeg;
import com.mapbox.directions.v5.models.StepManeuver;
import com.mapbox.geojson.Point;

public class MetricsRouteProgress {
  private DirectionsRoute directionsRoute;
  private double distanceRemaining;
  private double durationRemaining;
  private RouteLegProgress currentLegProgress;
  private double distanceTraveled;

  public MetricsRouteProgress(RouteProgress routeProgress) {
    this.directionsRoute = routeProgress.directionsRoute();
    this.distanceRemaining = routeProgress.distanceRemaining();
    this.durationRemaining = routeProgress.durationRemaining();
    this.currentLegProgress = routeProgress.currentLegProgress();
    this.distanceTraveled = routeProgress.distanceTraveled();
  }

  public double getDistanceRemaining() {
    return distanceRemaining;
  }

  public double getDurationRemaining() {
    return durationRemaining;
  }

  public double getDistanceTraveled() {
    return distanceTraveled;
  }

  public RouteLegProgress getCurrentLegProgress() {
    return currentLegProgress;
  }

  public int getDirectionsRouteDistance() {
    if (directionsRoute.distance() != null) {
      return directionsRoute.distance().intValue();
    }
    return 0;
  }

  public int getDirectionsRouteDuration() {
    if (directionsRoute.duration() != null) {
      return directionsRoute.duration().intValue();
    }
    return 0;
  }

  public String getDirectionsRouteGeometry() {
    if (directionsRoute.geometry() != null) {
      return directionsRoute.geometry();
    }
    return "";
  }

  public String getDirectionsRouteProfile() {
    if (directionsRoute.routeOptions() != null
      && directionsRoute.routeOptions().profile() != null) {
      return directionsRoute.routeOptions().profile();
    }
    return "";
  }

  public int getCurrentStepDistance() {
    if (currentLegProgress.currentStep().distance() != null) {
      return currentLegProgress.currentStep().distance().intValue();
    }
    return 0;
  }

  public int getCurrentStepDuration() {
    if (currentLegProgress.currentStep().duration() != null) {
      return currentLegProgress.currentStep().duration().intValue();
    }
    return 0;
  }

  public double getCurrentStepProgressDistanceRemaining() {
    if ((Double) currentLegProgress.currentStepProgress().distanceRemaining() != null) {
      return currentLegProgress.currentStepProgress().distanceRemaining();
    }
    return 0;
  }

  public double getCurrentStepProgressDurationRemaining() {
    if ((Double) currentLegProgress.currentStepProgress().durationRemaining() != null) {
      return currentLegProgress.currentStepProgress().durationRemaining();
    }
    return 0;
  }

  public String getCurrentStepName() {
    if (currentLegProgress.currentStep().name() != null) {
      return currentLegProgress.currentStep().name();
    }
    return "";
  }

  public Point getDirectionsRouteDestination() {
    RouteLeg lastLeg = directionsRoute.legs().get(directionsRoute.legs().size() - 1);
    LegStep lastStep = lastLeg.steps().get(lastLeg.steps().size() - 1);
    StepManeuver finalManuever = lastStep.maneuver();

    if (finalManuever.location() != null) {
      return finalManuever.location();
    }

    return Point.fromLngLat(0d, 0d);
  }
}

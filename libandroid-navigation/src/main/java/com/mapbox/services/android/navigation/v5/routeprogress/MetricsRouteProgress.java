package com.mapbox.services.android.navigation.v5.routeprogress;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.directions.v5.models.StepManeuver;
import com.mapbox.geojson.BoundingBox;
import com.mapbox.geojson.Point;

import java.util.List;

public class MetricsRouteProgress {
  private DirectionsRoute directionsRoute;
  private double distanceRemaining = 0;
  private double durationRemaining = 0;
  private RouteLegProgress currentLegProgress;
  private double distanceTraveled = 0;

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
    StepManeuver finalManuever = directionsRoute.legs().get(directionsRoute.legs().size() - 1).steps()
      .get(directionsRoute.legs().get(directionsRoute.legs().size() - 1).steps().size() - 1).maneuver();


    if (finalManuever.location() != null) {
      return finalManuever.location();
    }

    return new Point() {
      @Nullable
      @Override
      public BoundingBox bbox() {
        return null;
      }

      @NonNull
      @Override
      public List<Double> coordinates() {
        return null;
      }
    };
  }
}

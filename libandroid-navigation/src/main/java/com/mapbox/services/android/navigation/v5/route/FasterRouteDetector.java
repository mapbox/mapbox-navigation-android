package com.mapbox.services.android.navigation.v5.route;

import android.location.Location;

import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteStepProgress;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_CHECK_FASTER_ROUTE_INTERVAL;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_MEDIUM_ALERT_DURATION;

public class FasterRouteDetector extends FasterRoute {

  private static final int VALID_ROUTE_DURATION_REMAINING = 600;

  private Location lastCheckedLocation;

  @Override
  public boolean shouldCheckFasterRoute(Location location, RouteProgress routeProgress) {
    if (location == null || routeProgress == null) {
      return false;
    }
    // On first pass through detector, last checked location will be null
    if (lastCheckedLocation == null) {
      lastCheckedLocation = location;
    }
    // Check if the faster route time interval has been exceeded
    if (secondsSinceLastCheck(location) >= NAVIGATION_CHECK_FASTER_ROUTE_INTERVAL) {
      lastCheckedLocation = location;
      // Check for both valid route and step durations remaining
      if (validRouteDurationRemaining(routeProgress) && validStepDurationRemaining(routeProgress)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isFasterRoute(DirectionsResponse response, RouteProgress routeProgress) {
    if (validRouteResponse(response)) {

      double currentDurationRemaining = routeProgress.durationRemaining();
      DirectionsRoute newRoute = response.routes().get(0);

      if (hasLegs(newRoute)) {
        // Extract the first leg
        RouteLeg routeLeg = newRoute.legs().get(0);
        if (hasAtLeastTwoSteps(routeLeg)) {
          // Extract the first two steps
          LegStep firstStep = routeLeg.steps().get(0);
          LegStep secondStep = routeLeg.steps().get(1);
          // Check for valid first and second steps of the new route
          if (!validFirstStep(firstStep) || !validSecondStep(secondStep, routeProgress)) {
            return false;
          }
        }
      }
      // New route must be at least 10% faster
      if (newRoute.duration() <= (0.9 * currentDurationRemaining)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasLegs(DirectionsRoute newRoute) {
    return newRoute.legs() != null && !newRoute.legs().isEmpty();
  }

  private boolean hasAtLeastTwoSteps(RouteLeg routeLeg) {
    return routeLeg.steps() != null && routeLeg.steps().size() > 2;
  }

  /**
   * The second step of the new route is valid if
   * it equals the current route upcoming step.
   *
   * @param secondStep of the new route
   * @param routeProgress current route progress
   * @return true if valid, false if not
   */
  private boolean validSecondStep(LegStep secondStep, RouteProgress routeProgress) {
    return routeProgress.currentLegProgress().upComingStep() != null
      && routeProgress.currentLegProgress().upComingStep().equals(secondStep);
  }

  /**
   * First step is valid if it is greater than
   * {@link com.mapbox.services.android.navigation.v5.navigation.NavigationConstants#NAVIGATION_MEDIUM_ALERT_DURATION}.
   *
   * @param firstStep of the new route
   * @return true if valid, false if not
   */
  private boolean validFirstStep(LegStep firstStep) {
    return firstStep.duration() > NAVIGATION_MEDIUM_ALERT_DURATION;
  }

  /**
   * Checks if we have at least one {@link DirectionsRoute} in the given
   * {@link DirectionsResponse}.
   *
   * @param response to be checked
   * @return true if valid, false if not
   */
  private boolean validRouteResponse(DirectionsResponse response) {
    return response != null
      && !response.routes().isEmpty();
  }

  private boolean validRouteDurationRemaining(RouteProgress routeProgress) {
    // Total route duration remaining in seconds
    int routeDurationRemaining = (int) routeProgress.durationRemaining();
    return routeDurationRemaining > VALID_ROUTE_DURATION_REMAINING;
  }

  private boolean validStepDurationRemaining(RouteProgress routeProgress) {
    RouteStepProgress currentStepProgress = routeProgress.currentLegProgress().currentStepProgress();
    // Current step duration remaining in seconds
    int currentStepDurationRemaining = (int) currentStepProgress.durationRemaining();
    return currentStepDurationRemaining > NAVIGATION_MEDIUM_ALERT_DURATION;
  }

  private long secondsSinceLastCheck(Location location) {
    return dateDiff(new Date(lastCheckedLocation.getTime()), new Date(location.getTime()), TimeUnit.SECONDS);
  }

  private long dateDiff(Date firstDate, Date secondDate, TimeUnit timeUnit) {
    long diffInMillis = secondDate.getTime() - firstDate.getTime();
    return timeUnit.convert(diffInMillis, TimeUnit.MILLISECONDS);
  }
}

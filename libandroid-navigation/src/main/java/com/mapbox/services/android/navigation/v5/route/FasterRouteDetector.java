package com.mapbox.services.android.navigation.v5.route;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteStepProgress;
import com.mapbox.services.android.navigation.v5.utils.time.TimeUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_CHECK_FASTER_ROUTE_INTERVAL;

public class FasterRouteDetector extends FasterRoute {

  private static final int MEDIUM_ALERT_DURATION_REMAINING = 70;
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
    if (secondsSinceLastCheck(location) > NAVIGATION_CHECK_FASTER_ROUTE_INTERVAL) {
      lastCheckedLocation = location;
      // Check for both valid route and step durations remaining
      if (validRouteDurationRemaining(routeProgress) && validStepDurationRemaining(routeProgress)) {
        return true;
      }
    }
    return false;
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
    return currentStepDurationRemaining > MEDIUM_ALERT_DURATION_REMAINING;
  }

  private long secondsSinceLastCheck(Location location) {
    return TimeUtils.dateDiff(new Date(lastCheckedLocation.getTime()),
      new Date(location.getTime()), TimeUnit.SECONDS);
  }
}

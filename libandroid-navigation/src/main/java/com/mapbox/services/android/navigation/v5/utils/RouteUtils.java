package com.mapbox.services.android.navigation.v5.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.RouteOptions;
import com.mapbox.core.utils.TextUtils;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgressState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RouteUtils {
  private static final int FIRST_INSTRUCTION = 0;
  private static final int ORIGIN_WAYPOINT_NAME_THRESHOLD = 1;
  private static final int ORIGIN_WAYPOINT_NAME = 0;
  private static final int FIRST_POSITION = 0;
  private static final int SECOND_POSITION = 1;
  private static final String SEMICOLON = ";";

  /**
   * Looks at the current {@link RouteProgressState} and returns if
   * is {@link RouteProgressState#ROUTE_ARRIVED}.
   *
   * @param routeProgress the current route progress
   * @return true if in arrival state, false if not
   */
  public boolean isArrivalEvent(@NonNull RouteProgress routeProgress) {
    RouteProgressState currentState = routeProgress.currentState();
    return currentState != null && currentState == RouteProgressState.ROUTE_ARRIVED;
  }

  /**
   * Looks at the current {@link RouteProgress} list of legs and
   * checks if the current leg is the last leg.
   *
   * @param routeProgress the current route progress
   * @return true if last leg, false if not
   * @since 0.8.0
   */
  public boolean isLastLeg(RouteProgress routeProgress) {
    List<RouteLeg> legs = routeProgress.directionsRoute().legs();
    RouteLeg currentLeg = routeProgress.currentLeg();
    return currentLeg.equals(legs.get(legs.size() - 1));
  }

  /**
   * Given a {@link RouteProgress}, this method will calculate the remaining coordinates
   * along the given route based on total route coordinates and the progress remaining waypoints.
   * <p>
   * If the coordinate size is less than the remaining waypoints, this method
   * will return null.
   *
   * @param routeProgress for route coordinates and remaining waypoints
   * @return list of remaining waypoints as {@link Point}s
   * @since 0.10.0
   */
  @Nullable
  public List<Point> calculateRemainingWaypoints(RouteProgress routeProgress) {
    if (routeProgress.directionsRoute().routeOptions() == null) {
      return null;
    }
    List<Point> coordinates = new ArrayList<>(routeProgress.directionsRoute().routeOptions().coordinates());
    int coordinatesSize = coordinates.size();
    int remainingWaypoints = routeProgress.remainingWaypoints();
    if (coordinatesSize < remainingWaypoints) {
      return null;
    }
    List<Point> remainingCoordinates = coordinates.subList(coordinatesSize - remainingWaypoints, coordinatesSize);
    return remainingCoordinates;
  }

  /**
   * Given a {@link RouteProgress}, this method will calculate the remaining waypoint names
   * along the given route based on route option waypoint names and the progress remaining coordinates.
   * <p>
   * If the waypoint names are empty, this method will return null.
   *
   * @param routeProgress for route waypoint names and remaining coordinates
   * @return String array including the origin waypoint name and the remaining ones
   * @since 0.19.0
   */
  @Nullable
  public String[] calculateRemainingWaypointNames(RouteProgress routeProgress) {
    RouteOptions routeOptions = routeProgress.directionsRoute().routeOptions();
    if (routeOptions == null || TextUtils.isEmpty(routeOptions.waypointNames())) {
      return null;
    }
    String allWaypointNames = routeOptions.waypointNames();
    String[] names = allWaypointNames.split(SEMICOLON);
    int coordinatesSize = routeProgress.directionsRoute().routeOptions().coordinates().size();
    String[] remainingWaypointNames = Arrays.copyOfRange(names,
      coordinatesSize - routeProgress.remainingWaypoints(), coordinatesSize);
    String[] waypointNames = new String[remainingWaypointNames.length + ORIGIN_WAYPOINT_NAME_THRESHOLD];
    waypointNames[ORIGIN_WAYPOINT_NAME] = names[ORIGIN_WAYPOINT_NAME];
    System.arraycopy(remainingWaypointNames, FIRST_POSITION, waypointNames, SECOND_POSITION,
      remainingWaypointNames.length);
    return waypointNames;
  }

  /**
   * Given the current step / current step distance remaining, this function will
   * find the current instructions to be shown.
   *
   * @param currentStep           holding the current banner instructions
   * @param stepDistanceRemaining to determine progress along the currentStep
   * @return the current banner instructions based on the current distance along the step
   * @since 0.13.0
   */
  @Nullable
  public BannerInstructions findCurrentBannerInstructions(LegStep currentStep, double stepDistanceRemaining) {
    if (isValidBannerInstructions(currentStep)) {
      List<BannerInstructions> instructions = sortBannerInstructions(currentStep.bannerInstructions());
      for (BannerInstructions instruction : instructions) {
        int distanceAlongGeometry = (int) instruction.distanceAlongGeometry();
        if (distanceAlongGeometry >= (int) stepDistanceRemaining) {
          return instruction;
        }
      }
      return instructions.get(FIRST_INSTRUCTION);
    }
    return null;
  }

  private boolean isValidBannerInstructions(LegStep currentStep) {
    return isValidStep(currentStep) && hasInstructions(currentStep.bannerInstructions());
  }

  private List<BannerInstructions> sortBannerInstructions(List<BannerInstructions> instructions) {
    List<BannerInstructions> sortedInstructions = new ArrayList<>(instructions);
    Collections.sort(sortedInstructions, new Comparator<BannerInstructions>() {
      @Override
      public int compare(BannerInstructions instructions, BannerInstructions nextInstructions) {
        return Double.compare(instructions.distanceAlongGeometry(), nextInstructions.distanceAlongGeometry());
      }
    });
    return sortedInstructions;
  }

  private boolean isValidStep(LegStep step) {
    return step != null;
  }

  private <T> boolean hasInstructions(List<T> instructions) {
    return instructions != null && !instructions.isEmpty();
  }
}

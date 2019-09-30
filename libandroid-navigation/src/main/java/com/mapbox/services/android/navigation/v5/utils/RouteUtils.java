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

import timber.log.Timber;

public class RouteUtils {
  private static final int FIRST_INSTRUCTION = 0;
  private static final int ORIGIN_WAYPOINT_NAME_THRESHOLD = 1;
  private static final int ORIGIN_WAYPOINT_INDEX_THRESHOLD = 1;
  private static final int ORIGIN_WAYPOINT_INDEX = 0;
  private static final int ORIGIN_APPROACH_THRESHOLD = 1;
  private static final int ORIGIN_APPROACH_INDEX = 0;
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
    return currentState == RouteProgressState.ROUTE_ARRIVED;
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
   * Given a {@link RouteProgress}, this method will calculate the remaining waypoints coordinates
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
    RouteOptions routeOptions = routeProgress.directionsRoute().routeOptions();
    if (routeOptions == null) {
      return null;
    }
    List<Point> coordinates = new ArrayList<>(routeOptions.coordinates());
    int coordinatesSize = coordinates.size();
    int remainingWaypointsCount = routeProgress.remainingWaypointsCount();
    if (coordinatesSize < remainingWaypointsCount) {
      return null;
    }
    String waypointIndices = routeOptions.waypointIndices();
    if (waypointIndices == null) {
      return null;
    }
    String[] wayPointIndices = waypointIndices.split(SEMICOLON);
    String[] remainingWaypointIndices = Arrays.copyOfRange(wayPointIndices,
      wayPointIndices.length - remainingWaypointsCount, wayPointIndices.length);
    List<Point> remainingCoordinates = new ArrayList<>();
    try {
      int firstRemainingWaypointIndex = Integer.valueOf(remainingWaypointIndices[FIRST_POSITION]);
      remainingCoordinates = coordinates.subList(firstRemainingWaypointIndex, coordinatesSize);
    } catch (NumberFormatException ex) {
      Timber.e("Fail to convert waypoint index to integer");
    }
    return remainingCoordinates;
  }

  /**
   * Given a {@link RouteProgress}, this method will calculate the waypoint indices
   * along the given route based on route option waypoint indices and the progress remaining waypoints coordinates.
   * Remaining waypoint indices are recalculated based on count of already achieved waypoints.
   * <p>
   * If the waypoint indices are empty, this method will return null.
   *
   * @param routeProgress for route waypoint indices and remaining coordinates
   * @return Integer array including the origin waypoint index and the recalculated remaining ones
   * @since 0.43.0
   */
  @Nullable
  public Integer[] calculateRemainingWaypointIndices(RouteProgress routeProgress) {
    RouteOptions routeOptions = routeProgress.directionsRoute().routeOptions();
    if (routeOptions == null || TextUtils.isEmpty(routeOptions.waypointIndices())) {
      return null;
    }
    String waypointIndices = routeOptions.waypointIndices();
    if (waypointIndices == null) {
      return null;
    }
    int remainingWaypointsCount = routeProgress.remainingWaypointsCount();
    String[] allWaypointIndices = waypointIndices.split(SEMICOLON);
    String[] remainingWaypointIndices = Arrays.copyOfRange(allWaypointIndices,
      allWaypointIndices.length - remainingWaypointsCount, allWaypointIndices.length);
    Integer[] resultWaypointIndices = null;
    try {
      int firstRemainingWaypointIndex = Integer.valueOf(remainingWaypointIndices[FIRST_POSITION]);
      int traveledCoordinatesCount = firstRemainingWaypointIndex - ORIGIN_WAYPOINT_INDEX_THRESHOLD;
      resultWaypointIndices = new Integer[remainingWaypointIndices.length + ORIGIN_WAYPOINT_NAME_THRESHOLD];
      resultWaypointIndices[ORIGIN_WAYPOINT_INDEX] = Integer.valueOf(allWaypointIndices[ORIGIN_WAYPOINT_INDEX]);
      for (int i = 0; i < remainingWaypointIndices.length; i++) {
        resultWaypointIndices[i + 1] = Integer.valueOf(remainingWaypointIndices[i]) - traveledCoordinatesCount;
      }
    } catch (NumberFormatException ex) {
      Timber.e("Fail to convert waypoint index to integer");
    }
    return resultWaypointIndices;
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
    String waypointNames = routeOptions.waypointNames();
    if (waypointNames == null) {
      return null;
    }
    int remainingWaypointsCount = routeProgress.remainingWaypointsCount();
    String[] allWaypointNames = waypointNames.split(SEMICOLON);
    String[] remainingWaypointNames = Arrays.copyOfRange(allWaypointNames,
      allWaypointNames.length - remainingWaypointsCount, allWaypointNames.length);

    String[] resultWaypointNames = new String[remainingWaypointNames.length + ORIGIN_WAYPOINT_NAME_THRESHOLD];
    resultWaypointNames[ORIGIN_WAYPOINT_INDEX] = allWaypointNames[ORIGIN_WAYPOINT_INDEX];
    System.arraycopy(remainingWaypointNames, FIRST_POSITION, resultWaypointNames, SECOND_POSITION,
      remainingWaypointNames.length);
    return resultWaypointNames;
  }

  /**
   * Given a {@link RouteProgress}, this method will calculate the remaining approaches
   * along the given route based on route option approaches and the progress remaining approaches.
   * <p>
   * If the approaches are empty, this method will return null.
   *
   * @param routeProgress for route approaches and remaining coordinates
   * @return String array including the origin approach and the remaining ones
   * @since 0.19.0
   */
  @Nullable
  public String[] calculateRemainingApproaches(RouteProgress routeProgress) {
    RouteOptions routeOptions = routeProgress.directionsRoute().routeOptions();
    if (routeOptions == null) {
      return null;
    }
    String approaches = routeOptions.approaches();
    if (approaches == null || TextUtils.isEmpty(routeOptions.approaches())) {
      return null;
    }
    int remainingWaypointsCount = routeProgress.remainingWaypointsCount();
    String[] allApproaches = approaches.split(SEMICOLON);
    String[] remainingApproaches = Arrays.copyOfRange(allApproaches,
      allApproaches.length - remainingWaypointsCount, allApproaches.length);

    String[] resultApproaches = new String[remainingApproaches.length + ORIGIN_APPROACH_THRESHOLD];
    resultApproaches[ORIGIN_APPROACH_INDEX] = allApproaches[ORIGIN_APPROACH_INDEX];
    System.arraycopy(remainingApproaches, FIRST_POSITION, resultApproaches, SECOND_POSITION,
      remainingApproaches.length);
    return resultApproaches;
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

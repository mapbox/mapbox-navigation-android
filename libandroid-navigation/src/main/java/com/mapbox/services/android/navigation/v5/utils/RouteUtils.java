package com.mapbox.services.android.navigation.v5.utils;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.api.directions.v5.models.VoiceInstructions;
import com.mapbox.core.utils.TextUtils;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;

public class RouteUtils {

  private static final String FORCED_LOCATION = "Forced Location";
  private static final int FIRST_COORDINATE = 0;
  private static final int ZERO_INSTRUCTIONS = 0;
  private static final int FIRST_INSTRUCTION = 0;
  private static final Set<String> VALID_PROFILES = new HashSet<String>() {
    {
      add(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC);
      add(DirectionsCriteria.PROFILE_DRIVING);
      add(DirectionsCriteria.PROFILE_CYCLING);
      add(DirectionsCriteria.PROFILE_WALKING);
    }
  };

  /**
   * Compares a new routeProgress geometry to a previousRouteProgress geometry to determine if the
   * user is traversing along a new route. If the route geometries do not match, this returns true.
   *
   * @param previousRouteProgress the past route progress with the directions route included
   * @param routeProgress         the route progress with the directions route included
   * @return true if the direction route geometries do not match up, otherwise, false
   * @since 0.7.0
   */
  public boolean isNewRoute(@Nullable RouteProgress previousRouteProgress,
                                   @NonNull RouteProgress routeProgress) {
    return isNewRoute(previousRouteProgress, routeProgress.directionsRoute());
  }

  /**
   * Compares a new routeProgress geometry to a previousRouteProgress geometry to determine if the
   * user is traversing along a new route. If the route geometries do not match, this returns true.
   *
   * @param previousRouteProgress the past route progress with the directions route included
   * @param directionsRoute       the current directions route
   * @return true if the direction route geometries do not match up, otherwise, false
   * @since 0.7.0
   */
  public boolean isNewRoute(@Nullable RouteProgress previousRouteProgress,
                                   @NonNull DirectionsRoute directionsRoute) {
    return previousRouteProgress == null || !previousRouteProgress.directionsRoute().geometry()
      .equals(directionsRoute.geometry());
  }

  /**
   * Looks at the current {@link RouteProgress} maneuverType for type "arrival", then
   * checks if the arrival meter threshold has been hit.
   *
   * @param routeProgress the current route progress
   * @param milestone     the current milestone from the MilestoneEventListener
   * @return true if in arrival state, false if not
   * @since 0.8.0
   */
  public boolean isArrivalEvent(@NonNull RouteProgress routeProgress, @NonNull Milestone milestone) {
    if (!(milestone instanceof BannerInstructionMilestone)) {
      return false;
    }
    boolean isValidArrivalManeuverType = upcomingStepIsArrivalManeuverType(routeProgress)
      || currentStepIsArrivalManeuverType(routeProgress);
    if (isValidArrivalManeuverType) {
      LegStep currentStep = routeProgress.currentLegProgress().currentStep();
      BannerInstructions currentInstructions = ((BannerInstructionMilestone) milestone).getBannerInstructions();
      List<BannerInstructions> bannerInstructions = currentStep.bannerInstructions();
      if (hasValidInstructions(bannerInstructions, currentInstructions)) {
        int lastInstructionIndex = bannerInstructions.size() - 1;
        BannerInstructions lastInstructions = bannerInstructions.get(lastInstructionIndex);
        return currentInstructions.equals(lastInstructions);
      }
    }
    return false;
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
    if (coordinates.size() < routeProgress.remainingWaypoints()) {
      return null;
    }
    coordinates.subList(0, routeProgress.remainingWaypoints()).clear();
    return coordinates;
  }

  /**
   * If navigation begins, a location update is sometimes needed to force a
   * progress change update as soon as navigation is started.
   * <p>
   * This method creates a location update from the first coordinate (origin) that created
   * the route.
   *
   * @param route with list of coordinates
   * @return {@link Location} from first coordinate
   * @since 0.10.0
   */
  public Location createFirstLocationFromRoute(DirectionsRoute route) {
    List<Point> coordinates = route.routeOptions().coordinates();
    Point origin = coordinates.get(FIRST_COORDINATE);
    Location forcedLocation = new Location(FORCED_LOCATION);
    forcedLocation.setLatitude(origin.latitude());
    forcedLocation.setLongitude(origin.longitude());
    return forcedLocation;
  }

  /**
   * Checks if the {@link String} route profile provided is a valid profile
   * that can be used with the directions API.
   *
   * @param routeProfile being validated
   * @return true if valid, false if not
   * @since 0.13.0
   */
  public boolean isValidRouteProfile(String routeProfile) {
    return !TextUtils.isEmpty(routeProfile) && VALID_PROFILES.contains(routeProfile);
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
        double distanceAlongGeometry = instruction.distanceAlongGeometry();
        if (distanceAlongGeometry >= stepDistanceRemaining) {
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

  /**
   * This method returns the current {@link BannerText} based on the currentStep distance
   * remaining.
   * <p>
   * When called, this is the banner text that should be shown at the given point along the route.
   *
   * @param currentStep           holding the current banner instructions
   * @param stepDistanceRemaining to determine progress along the currentStep
   * @param findPrimary           if the primary or secondary BannerText should be retrieved
   * @return current BannerText based on currentStep distance remaining
   * @since 0.13.0
   */
  @Nullable
  public BannerText findCurrentBannerText(LegStep currentStep, double stepDistanceRemaining,
                                                 boolean findPrimary) {
    BannerInstructions instructions = findCurrentBannerInstructions(currentStep, stepDistanceRemaining);
    if (instructions != null) {
      return retrievePrimaryOrSecondaryBannerText(findPrimary, instructions);
    }
    return null;
  }

  /**
   * This method returns the current {@link VoiceInstructions} based on the step distance
   * remaining.
   *
   * @param currentStep           holding the current banner instructions
   * @param stepDistanceRemaining to determine progress along the step
   * @return current voice instructions based on step distance remaining
   * @since 0.13.0
   */
  @Nullable
  public VoiceInstructions findCurrentVoiceInstructions(LegStep currentStep, double stepDistanceRemaining) {
    if (isValidVoiceInstructions(currentStep)) {
      List<VoiceInstructions> instructions = sortVoiceInstructions(currentStep.voiceInstructions());
      for (VoiceInstructions instruction : instructions) {
        double distanceAlongGeometry = instruction.distanceAlongGeometry();
        if (distanceAlongGeometry >= stepDistanceRemaining) {
          return instruction;
        }
      }
      return instructions.get(FIRST_INSTRUCTION);
    }
    return null;
  }

  private boolean isValidVoiceInstructions(LegStep currentStep) {
    return isValidStep(currentStep) && hasInstructions(currentStep.voiceInstructions());
  }

  private List<VoiceInstructions> sortVoiceInstructions(List<VoiceInstructions>  instructions) {
    List<VoiceInstructions> sortedInstructions = new ArrayList<>(instructions);
    Collections.sort(sortedInstructions, new Comparator<VoiceInstructions>() {
      @Override
      public int compare(VoiceInstructions instructions, VoiceInstructions nextInstructions) {
        return Double.compare(instructions.distanceAlongGeometry(), nextInstructions.distanceAlongGeometry());
      }
    });
    return sortedInstructions;
  }

  private boolean upcomingStepIsArrivalManeuverType(@NonNull RouteProgress routeProgress) {
    return routeProgress.currentLegProgress().upComingStep() != null
      && routeProgress.currentLegProgress().upComingStep().maneuver().type().contains(STEP_MANEUVER_TYPE_ARRIVE);
  }

  private boolean currentStepIsArrivalManeuverType(@NonNull RouteProgress routeProgress) {
    return routeProgress.currentLegProgress().currentStep().maneuver().type().contains(STEP_MANEUVER_TYPE_ARRIVE);
  }

  private boolean isValidStep(LegStep step) {
    return step != null;
  }

  private <T> boolean hasInstructions(List<T> instructions) {
    return instructions != null && !instructions.isEmpty();
  }

  private <T> int checkValidIndex(List<T> instructions) {
    int instructionIndex = instructions.size() - 1;
    if (instructionIndex < 0) {
      instructionIndex = 0;
    }
    return instructionIndex;
  }

  private boolean hasValidInstructions(List<BannerInstructions> bannerInstructions,
                                              BannerInstructions currentInstructions) {
    return bannerInstructions != null && !bannerInstructions.isEmpty() && currentInstructions != null;
  }

  private BannerText retrievePrimaryOrSecondaryBannerText(boolean findPrimary, BannerInstructions instruction) {
    return findPrimary ? instruction.primary() : instruction.secondary();
  }
}

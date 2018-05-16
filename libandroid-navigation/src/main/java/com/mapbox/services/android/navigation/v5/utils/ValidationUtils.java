package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteOptions;

import java.util.MissingFormatArgumentException;

public final class ValidationUtils {

  private ValidationUtils() {
    // Class should not be initialized.
  }

  public static void validDirectionsRoute(DirectionsRoute directionsRoute,
                                          boolean defaultMilestonesEnabled) {
    if (defaultMilestonesEnabled) {
      RouteOptions routeOptions = directionsRoute.routeOptions();
      checkNullRouteOptions(routeOptions);
      checkInvalidVoiceInstructions(routeOptions);
      checkInvalidBannerInstructions(routeOptions);
    }
  }

  private static void checkNullRouteOptions(RouteOptions routeOptions) {
    if (routeOptions == null) {
      throw new MissingFormatArgumentException("Using the default milestones requires the "
        + "directions route to include the route options object.");
    }
  }

  private static void checkInvalidVoiceInstructions(RouteOptions routeOptions) {
    Boolean instructions = routeOptions.voiceInstructions();
    boolean invalidVoiceInstructions = instructions == null
      || !instructions;
    if (invalidVoiceInstructions) {
      throw new MissingFormatArgumentException("Using the default milestones requires the "
        + "directions route to be requested with voice instructions enabled.");
    }
  }

  private static void checkInvalidBannerInstructions(RouteOptions routeOptions) {
    Boolean instructions = routeOptions.bannerInstructions();
    boolean invalidBannerInstructions = instructions == null || !instructions;
    if (invalidBannerInstructions) {
      throw new MissingFormatArgumentException("Using the default milestones requires the "
        + "directions route to be requested with banner instructions enabled.");
    }
  }
}

package com.mapbox.services.android.navigation.v5.utils;

import com.mapbox.api.directions.v5.models.DirectionsRoute;

import java.util.MissingFormatArgumentException;

public final class ValidationUtils {

  private ValidationUtils() {
    // Class should not be initialized.
  }

  public static void validDirectionsRoute(DirectionsRoute directionsRoute,
                                          boolean defaultMilestonesEnabled) {
    if (defaultMilestonesEnabled
      && directionsRoute.routeOptions() != null
      && !directionsRoute.routeOptions().voiceInstructions()) {
      throw new MissingFormatArgumentException("Using the default milestone requires the "
        + "directions route to include the voice instructions object.");
    }
  }
}

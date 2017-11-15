package com.mapbox.services.android.navigation.v5.navigation;


import android.content.Context;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.BuildConfig;
import com.mapbox.services.android.navigation.v5.exception.NavigationException;
import com.mapbox.services.android.telemetry.MapboxEvent;
import com.mapbox.services.android.telemetry.MapboxTelemetry;
import com.mapbox.services.utils.TextUtils;

import java.util.Locale;

class Telemetry {
  private static final String MAPBOX_NAVIGATION_SDK_IDENTIFIER = "mapbox-navigation-android";
  private static final String MAPBOX_NAVIGATION_UI_SDK_IDENTIFIER = "mapbox-navigation-ui-android";

  void initialize(@NonNull Context context, @NonNull String accessToken, boolean isFromNavigationUi,
                  boolean debugLoggingEnabled) {
    validateAccessToken(accessToken);
    String sdkIdentifier = MAPBOX_NAVIGATION_SDK_IDENTIFIER;
    if (isFromNavigationUi) {
      sdkIdentifier = MAPBOX_NAVIGATION_UI_SDK_IDENTIFIER;
    }
    String userAgent = String.format("%s/%s", sdkIdentifier, BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME);
    MapboxTelemetry.getInstance().initialize(context, accessToken, userAgent, sdkIdentifier,
      BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME);
    MapboxTelemetry.getInstance().newUserAgent(userAgent);

    // Enable extra logging in debug mode
    MapboxTelemetry.getInstance().setDebugLoggingEnabled(debugLoggingEnabled);

    NavigationMetricsWrapper.sdkIdentifier = sdkIdentifier;
    NavigationMetricsWrapper.turnstileEvent();
    // TODO This should be removed when we figure out a solution in Telemetry
    // Force pushing a TYPE_MAP_LOAD event to ensure that the Nav turnstile event is sent
    MapboxTelemetry.getInstance().pushEvent(MapboxEvent.buildMapLoadEvent());
  }

  private void validateAccessToken(String accessToken) {
    if (TextUtils.isEmpty(accessToken) || (!accessToken.toLowerCase(Locale.US).startsWith("pk.")
      && !accessToken.toLowerCase(Locale.US).startsWith("sk."))) {
      throw new NavigationException("A valid access token must be passed in when first initializing"
        + " MapboxNavigation");
    }
  }
}

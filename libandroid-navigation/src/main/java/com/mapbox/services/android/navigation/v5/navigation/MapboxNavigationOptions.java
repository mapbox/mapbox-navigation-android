package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants
  .NAVIGATION_LOCATION_ENGINE_INTERVAL_LAG;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ROUNDING_INCREMENT_FIFTY;

/**
 * Immutable and can't be changed after passing into {@link MapboxNavigation}.
 */
@AutoValue
public abstract class MapboxNavigationOptions {

  public abstract boolean defaultMilestonesEnabled();

  public abstract boolean enableFasterRouteDetection();

  public abstract boolean enableAutoIncrementLegIndex();

  public abstract boolean isFromNavigationUi();

  public abstract boolean isDebugLoggingEnabled();

  @Nullable
  public abstract NavigationNotification navigationNotification();

  @NavigationConstants.RoundingIncrement
  public abstract int roundingIncrement();

  @NavigationTimeFormat.Type
  public abstract int timeFormatType();

  public abstract int navigationLocationEngineIntervalLagInMilliseconds();

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder defaultMilestonesEnabled(boolean defaultMilestonesEnabled);

    public abstract Builder enableFasterRouteDetection(boolean enableFasterRouteDetection);

    public abstract Builder enableAutoIncrementLegIndex(boolean enableAutoIncrementLegIndex);

    public abstract Builder isFromNavigationUi(boolean isFromNavigationUi);

    public abstract Builder isDebugLoggingEnabled(boolean debugLoggingEnabled);

    public abstract Builder navigationNotification(NavigationNotification notification);

    public abstract Builder roundingIncrement(@NavigationConstants.RoundingIncrement int roundingIncrement);

    public abstract Builder timeFormatType(@NavigationTimeFormat.Type int type);

    public abstract Builder navigationLocationEngineIntervalLagInMilliseconds(int lagInMilliseconds);

    public abstract MapboxNavigationOptions build();
  }

  public static Builder builder() {
    return new AutoValue_MapboxNavigationOptions.Builder()
      .enableFasterRouteDetection(false)
      .enableAutoIncrementLegIndex(true)
      .defaultMilestonesEnabled(true)
      .isFromNavigationUi(false)
      .isDebugLoggingEnabled(false)
      .roundingIncrement(ROUNDING_INCREMENT_FIFTY)
      .timeFormatType(NavigationTimeFormat.NONE_SPECIFIED)
      .navigationLocationEngineIntervalLagInMilliseconds(NAVIGATION_LOCATION_ENGINE_INTERVAL_LAG);
  }
}

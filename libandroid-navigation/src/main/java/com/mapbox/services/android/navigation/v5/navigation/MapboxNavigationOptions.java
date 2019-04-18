package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.services.android.navigation.R;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.NAVIGATION_LOCATION_ENGINE_INTERVAL_LAG;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ROUNDING_INCREMENT_FIFTY;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ROUTE_REFRESH_INTERVAL;

/**
 * Immutable and can't be changed after passing into {@link MapboxNavigation}.
 */
@AutoValue
public abstract class MapboxNavigationOptions {

  public abstract boolean defaultMilestonesEnabled();

  public abstract boolean enableFasterRouteDetection();

  public abstract boolean enableAutoIncrementLegIndex();

  /**
   * This value indicates if route refresh is enabled or disabled.
   *
   * @return whether route refresh is enabled or not
   */
  public abstract boolean enableRefreshRoute();

  /**
   * This value indicates the route refresh interval.
   *
   * @return route refresh interval in milliseconds
   */
  public abstract long refreshIntervalInMilliseconds();

  public abstract boolean isFromNavigationUi();

  public abstract boolean isDebugLoggingEnabled();

  @Nullable
  public abstract NavigationNotification navigationNotification();

  @NavigationConstants.RoundingIncrement
  public abstract int roundingIncrement();

  @NavigationTimeFormat.Type
  public abstract int timeFormatType();

  public abstract int navigationLocationEngineIntervalLagInMilliseconds();

  /**
   * The color resource id for the default notification.  This will be ignored
   * if a {@link NavigationNotification} is set.
   *
   * @return color resource id for notification
   */
  @ColorRes
  public abstract int defaultNotificationColorId();

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder defaultMilestonesEnabled(boolean defaultMilestonesEnabled);

    public abstract Builder enableFasterRouteDetection(boolean enableFasterRouteDetection);

    public abstract Builder enableAutoIncrementLegIndex(boolean enableAutoIncrementLegIndex);

    /**
     * This enables / disables refresh route. If not specified, it's enabled by default.
     *
     * @param enableRefreshRoute whether or not to enable route refresh
     * @return this builder for chaining options together
     */
    public abstract Builder enableRefreshRoute(boolean enableRefreshRoute);

    /**
     * This sets the route refresh interval. If not specified, the interval is 5 minutes by default.
     *
     * @param intervalInMilliseconds for route refresh
     * @return this builder for chaining options together
     */
    public abstract Builder refreshIntervalInMilliseconds(long intervalInMilliseconds);

    public abstract Builder isFromNavigationUi(boolean isFromNavigationUi);

    public abstract Builder isDebugLoggingEnabled(boolean debugLoggingEnabled);

    public abstract Builder navigationNotification(NavigationNotification notification);

    public abstract Builder roundingIncrement(@NavigationConstants.RoundingIncrement int roundingIncrement);

    public abstract Builder timeFormatType(@NavigationTimeFormat.Type int type);

    public abstract Builder navigationLocationEngineIntervalLagInMilliseconds(int lagInMilliseconds);

    /**
     * Optionally, set the background color of the default notification.
     *
     * @param defaultNotificationColorId the color resource to be used
     * @return this builder for chaining operations together
     */
    public abstract Builder defaultNotificationColorId(@ColorRes int defaultNotificationColorId);

    public abstract MapboxNavigationOptions build();
  }

  public static Builder builder() {
    return new AutoValue_MapboxNavigationOptions.Builder()
      .enableFasterRouteDetection(false)
      .enableAutoIncrementLegIndex(true)
      .enableRefreshRoute(true)
      .refreshIntervalInMilliseconds(ROUTE_REFRESH_INTERVAL)
      .defaultMilestonesEnabled(true)
      .isFromNavigationUi(false)
      .isDebugLoggingEnabled(false)
      .roundingIncrement(ROUNDING_INCREMENT_FIFTY)
      .timeFormatType(NavigationTimeFormat.NONE_SPECIFIED)
      .navigationLocationEngineIntervalLagInMilliseconds(NAVIGATION_LOCATION_ENGINE_INTERVAL_LAG)
      .defaultNotificationColorId(R.color.mapboxNotificationBlue);
  }
}

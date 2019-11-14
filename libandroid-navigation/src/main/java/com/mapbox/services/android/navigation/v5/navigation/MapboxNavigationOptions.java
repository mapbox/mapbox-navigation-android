package com.mapbox.services.android.navigation.v5.navigation;


import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ROUNDING_INCREMENT_FIFTY;

/**
 * Immutable and can't be changed after passing into {@link MapboxNavigation}.
 */
@AutoValue
public abstract class MapboxNavigationOptions {

  public abstract double maxTurnCompletionOffset();

  public abstract double maneuverZoneRadius();

  public abstract double maximumDistanceOffRoute();

  public abstract double deadReckoningTimeInterval();

  public abstract double maxManipulatedCourseAngle();

  public abstract double userLocationSnapDistance();

  public abstract int secondsBeforeReroute();

  public abstract boolean defaultMilestonesEnabled();

  public abstract boolean snapToRoute();

  public abstract boolean enableOffRouteDetection();

  public abstract boolean enableFasterRouteDetection();

  public abstract boolean manuallyEndNavigationUponCompletion();

  public abstract double metersRemainingTillArrival();

  public abstract boolean isFromNavigationUi();

  public abstract double minimumDistanceBeforeRerouting();

  public abstract boolean isDebugLoggingEnabled();

  @Nullable
  public abstract NavigationNotification navigationNotification();

  @NavigationConstants.RoundingIncrement
  public abstract int roundingIncrement();

  @NavigationTimeFormat.Type
  public abstract int timeFormatType();

  public abstract int locationAcceptableAccuracyInMetersThreshold();

  public abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder maxTurnCompletionOffset(double maxTurnCompletionOffset);

    public abstract Builder maneuverZoneRadius(double maneuverZoneRadius);

    public abstract Builder maximumDistanceOffRoute(double maximumDistanceOffRoute);

    public abstract Builder deadReckoningTimeInterval(double deadReckoningTimeInterval);

    public abstract Builder maxManipulatedCourseAngle(double maxManipulatedCourseAngle);

    public abstract Builder userLocationSnapDistance(double userLocationSnapDistance);

    public abstract Builder secondsBeforeReroute(int secondsBeforeReroute);

    public abstract Builder defaultMilestonesEnabled(boolean defaultMilestonesEnabled);

    public abstract Builder snapToRoute(boolean snapToRoute);

    public abstract Builder enableOffRouteDetection(boolean enableOffRouteDetection);

    public abstract Builder enableFasterRouteDetection(boolean enableFasterRouteDetection);

    public abstract Builder manuallyEndNavigationUponCompletion(boolean manuallyEndNavigation);

    public abstract Builder metersRemainingTillArrival(double metersRemainingTillArrival);

    public abstract Builder isFromNavigationUi(boolean isFromNavigationUi);

    public abstract Builder minimumDistanceBeforeRerouting(double distanceInMeters);

    public abstract Builder isDebugLoggingEnabled(boolean debugLoggingEnabled);

    public abstract Builder navigationNotification(NavigationNotification notification);

    public abstract Builder roundingIncrement(@NavigationConstants.RoundingIncrement int roundingIncrement);

    public abstract Builder timeFormatType(@NavigationTimeFormat.Type int type);

    public abstract Builder locationAcceptableAccuracyInMetersThreshold(int accuracyInMetersThreshold);

    public abstract MapboxNavigationOptions build();
  }

  public static Builder builder() {
    return new AutoValue_MapboxNavigationOptions.Builder()
      .maxTurnCompletionOffset(NavigationConstants.MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION)
      .maneuverZoneRadius(NavigationConstants.MANEUVER_ZONE_RADIUS)
      .maximumDistanceOffRoute(NavigationConstants.MAXIMUM_DISTANCE_BEFORE_OFF_ROUTE)
      .deadReckoningTimeInterval(NavigationConstants.DEAD_RECKONING_TIME_INTERVAL)
      .maxManipulatedCourseAngle(NavigationConstants.MAX_MANIPULATED_COURSE_ANGLE)
      .userLocationSnapDistance(NavigationConstants.USER_LOCATION_SNAPPING_DISTANCE)
      .secondsBeforeReroute(NavigationConstants.SECONDS_BEFORE_REROUTE)
      .enableOffRouteDetection(true)
      .enableFasterRouteDetection(false)
      .snapToRoute(true)
      .manuallyEndNavigationUponCompletion(false)
      .defaultMilestonesEnabled(true)
      .minimumDistanceBeforeRerouting(NavigationConstants.MINIMUM_DISTANCE_BEFORE_REROUTING)
      .metersRemainingTillArrival(NavigationConstants.METERS_REMAINING_TILL_ARRIVAL)
      .isFromNavigationUi(false)
      .isDebugLoggingEnabled(false)
      .roundingIncrement(ROUNDING_INCREMENT_FIFTY)
      .timeFormatType(NavigationTimeFormat.NONE_SPECIFIED)
      .locationAcceptableAccuracyInMetersThreshold(NavigationConstants.ONE_HUNDRED_METER_ACCEPTABLE_ACCURACY_THRESHOLD);
  }
}

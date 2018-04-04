package com.mapbox.services.android.navigation.v5.location;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

public class LocationValidator {

  private static final int ONE_SECOND_IN_MILLIS = 1000;

  private Location lastValidLocation;
  private int locationAcceptableAccuracyInMetersThreshold;
  private int locationAccuracyPercentThreshold;
  private int locationUpdateTimeInMillisThreshold;
  private int locationVelocityInMetersPerSecondThreshold;

  public LocationValidator(int locationAcceptableAccuracyInMetersThreshold, int locationAccuracyPercentThreshold,
                           int locationUpdateTimeInMillisThreshold, int locationVelocityInMetersPerSecondThreshold) {
    this.locationAcceptableAccuracyInMetersThreshold = locationAcceptableAccuracyInMetersThreshold;
    this.locationAccuracyPercentThreshold = locationAccuracyPercentThreshold;
    this.locationUpdateTimeInMillisThreshold = locationUpdateTimeInMillisThreshold;
    this.locationVelocityInMetersPerSecondThreshold = locationVelocityInMetersPerSecondThreshold;
  }

  public boolean isValidUpdate(@NonNull Location location) {
    if (checkLastValidLocation(location)) {
      return true;
    }
    long timeSinceLastValidUpdate = location.getTime() - lastValidLocation.getTime();

    boolean accuracyAcceptable = isAccuracyAcceptable(location);
    boolean validVelocity = isValidVelocity(location, timeSinceLastValidUpdate);
    boolean validLocation = isValidLocation(accuracyAcceptable, timeSinceLastValidUpdate);

    if (validVelocity && validLocation) {
      lastValidLocation = location;
      return true;
    }
    return false;
  }

  /**
   * On the first location update, the last valid location will be null.
   * <p>
   * So set the last valid location and return true.  On the next update, there
   * will be a last update to compare against.
   *
   * @param location new location update
   * @return true if last valid location null, false otherwise
   */
  private boolean checkLastValidLocation(@NonNull Location location) {
    if (lastValidLocation == null) {
      lastValidLocation = location;
      return true;
    }
    return false;
  }

  /**
   * New location accuracy is acceptable if it is less than or equal to
   * {@link LocationValidator#locationAcceptableAccuracyInMetersThreshold}.
   * <p>
   * Otherwise, new location update is acceptable, even with worse accuracy, if it is from
   * the same provider and is no more than {@link LocationValidator#locationAccuracyPercentThreshold} worse.
   *
   * @param location new location received
   * @return true if acceptable accuracy, false otherwise
   */
  private boolean isAccuracyAcceptable(@NonNull Location location) {
    float currentAccuracy = location.getAccuracy();
    if (currentAccuracy <= locationAcceptableAccuracyInMetersThreshold) {
      return true;
    }

    float previousAccuracy = lastValidLocation.getAccuracy();
    float accuracyDifference = Math.abs(previousAccuracy - currentAccuracy);

    boolean improvedAccuracy = currentAccuracy <= previousAccuracy;
    boolean currentAccuracyWorse = currentAccuracy > previousAccuracy;
    boolean hasSameProvider = lastValidLocation.getProvider().equals(location.getProvider());
    double percentThreshold = locationAccuracyPercentThreshold / 100.0;
    boolean lessThanPercentThreshold = (accuracyDifference <= (previousAccuracy * percentThreshold));
    boolean lessAccuracyAcceptable = currentAccuracyWorse && hasSameProvider && lessThanPercentThreshold;

    return improvedAccuracy || lessAccuracyAcceptable;
  }

  /**
   * Calculates the velocity between the new location update and the last update
   * that has been stored.
   * <p>
   * Average velocity = distance traveled over time (distance / time).
   *
   * @param location                 new location received
   * @param timeSinceLastValidUpdate in millis, how long it has been since the new and last update
   * @return true if valid velocity, false otherwise
   */
  private boolean isValidVelocity(@NonNull Location location, long timeSinceLastValidUpdate) {
    Point currentPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    Point previousValidPoint = Point.fromLngLat(lastValidLocation.getLongitude(), lastValidLocation.getLatitude());
    double distanceInMeters = TurfMeasurement.distance(previousValidPoint, currentPoint, TurfConstants.UNIT_METERS);

    double velocityInMetersPerSecond = distanceInMeters / (timeSinceLastValidUpdate / ONE_SECOND_IN_MILLIS);

    return velocityInMetersPerSecond <= locationVelocityInMetersPerSecondThreshold;
  }

  /**
   * Location update is valid if it has better accuracy, has been over 5 seconds since the last update,
   * or the new update has worse accuracy but it is still acceptable.
   *
   * @param accuracyAcceptable       new location accuracy is acceptable or not
   * @param timeSinceLastValidUpdate in millis, how long it has been since the new and last update
   * @return true if valid location, false otherwise
   */
  private boolean isValidLocation(boolean accuracyAcceptable, long timeSinceLastValidUpdate) {
    return accuracyAcceptable || timeSinceLastValidUpdate > locationUpdateTimeInMillisThreshold;
  }
}

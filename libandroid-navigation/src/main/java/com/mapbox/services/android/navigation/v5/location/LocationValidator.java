package com.mapbox.services.android.navigation.v5.location;

import android.location.Location;

import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class LocationValidator {

  private static final int TIME_THRESHOLD = 5000; // 5 seconds
  private static final int ACCURACY_THRESHOLD = 10; // percent
  private static final int VELOCITY_THRESHOLD = 200; // milliseconds

  private static LocationValidator instance;
  private Location previousValidLocation;
  private boolean loggingEnabled;

  /**
   * Primary access method (using singleton pattern)
   *
   * @return LocationValidator
   */
  public static synchronized LocationValidator getInstance() {
    if (instance == null) {
      instance = new LocationValidator();
    }

    return instance;
  }

  public boolean isValidUpdate(Location location) {
    // First update
    if (previousValidLocation == null) {
      previousValidLocation = location;
      return true;
    }

    float currentAccuracy = location.getAccuracy();
    float previousAccuracy = previousValidLocation.getAccuracy();
    float accuracyDifference = Math.abs(previousAccuracy - currentAccuracy);

    log("Accuracy - Current: %s, Previous: %s, Difference: %s",
      currentAccuracy, previousAccuracy, accuracyDifference);

    boolean currentAccuracyWorse = currentAccuracy > previousAccuracy;
    boolean hasSameProvider = previousValidLocation.getProvider().equals(location.getProvider());
    boolean lessThanPercentThreshold = (accuracyDifference <= (previousAccuracy / ACCURACY_THRESHOLD));

    // New location update is acceptable, even with worse accuracy, if it is from
    // the same provider and is no more than 10% worse
    boolean worseAccuracyAcceptable = currentAccuracyWorse && hasSameProvider && lessThanPercentThreshold;

    log("Worse accuracy acceptable: %s", worseAccuracyAcceptable);

    // Calculate the velocity between these two points
    Point currentPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    Point previousValidPoint = Point.fromLngLat(previousValidLocation.getLongitude(),
      previousValidLocation.getLatitude());
    double distanceInMeters = TurfMeasurement.distance(previousValidPoint, currentPoint, TurfConstants.UNIT_METERS);

    log("Distance between updates: %s", distanceInMeters);

    // Average velocity = distance traveled over time
    long timeSinceLastValidUpdate = location.getTime() - previousValidLocation.getTime();
    double velocity = distanceInMeters / timeSinceLastValidUpdate / 1000;

    log("Time since last update: %s seconds", TimeUnit.MILLISECONDS.toSeconds(timeSinceLastValidUpdate));
    log("Velocity %s", velocity);

    boolean validVelocity = velocity <= VELOCITY_THRESHOLD;

    log("Valid velocity: %s", validVelocity);

    // Location update is valid if it has better accuracy, has been over 5 seconds since the last update,
    // or the new update has worse accuracy but it is still acceptable
    boolean validLocation = currentAccuracy <= previousAccuracy
      || timeSinceLastValidUpdate > TIME_THRESHOLD
      || worseAccuracyAcceptable;

    log("Valid location: %s", validLocation);

    // Valid velocity and location, set last valid location
    if (validVelocity && validLocation) {
      previousValidLocation = location;
      return true;
    }

    // Not a valid update
    return false;
  }

  public void setLocationLoggingEnabled(boolean loggingEnabled) {
    this.loggingEnabled = loggingEnabled;
  }

  private void log(String message, Object... args) {
    if (loggingEnabled) {
      Timber.d(message, args);
    }
  }
}

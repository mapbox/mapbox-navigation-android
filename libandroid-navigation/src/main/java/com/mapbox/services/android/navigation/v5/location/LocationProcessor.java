package com.mapbox.services.android.navigation.v5.location;

import android.location.Location;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.utils.RingBuffer;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class LocationProcessor {

  private static final int TIME_THRESHOLD = 5000; // 5 seconds
  private static final int ACCURACY_THRESHOLD = 10; // percent
  private static final int VELOCITY_THRESHOLD = 200; // milliseconds

  private static LocationProcessor instance;
  private RingBuffer<Location> locations;
  private boolean loggingEnabled;

  /**
   * Primary access method (using singleton pattern)
   *
   * @return LocationProcessor
   */
  public static synchronized LocationProcessor getInstance() {
    if (instance == null) {
      instance = new LocationProcessor();
    }

    return instance;
  }

  private LocationProcessor() {
    // Create a buffer for the last five location updates
    locations = new RingBuffer<>(5);
  }

  public boolean isValidUpdate(Location location) {
    // First update
    if (locations.isEmpty()) {
      locations.add(location);
      return true;
    }

    float currentAccuracy = location.getAccuracy();
    float previousAccuracy = locations.peekLast().getAccuracy();
    float accuracyDifference = Math.abs(previousAccuracy - currentAccuracy);

    log("Accuracy - Current: %s, Previous: %s, Difference: %s",
      currentAccuracy, previousAccuracy, accuracyDifference);

    boolean currentAccuracyWorse = currentAccuracy > previousAccuracy;
    boolean hasSameProvider = getLastValidLocation().getProvider().equals(location.getProvider());
    boolean lessThanPercentThreshold = (accuracyDifference <= (previousAccuracy / ACCURACY_THRESHOLD));

    // New location update is acceptable, even with worse accuracy, if it is from
    // the same provider and is no more than 10% worse
    boolean worseAccuracyAcceptable = currentAccuracyWorse && hasSameProvider && lessThanPercentThreshold;

    log("Worse accuracy acceptable: %s", worseAccuracyAcceptable);

    // Calculate the velocity between these two points
    Point currentPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    Point previousValidPoint = Point.fromLngLat(getLastValidLocation().getLongitude(),
      getLastValidLocation().getLatitude());
    double distanceInMeters = TurfMeasurement.distance(previousValidPoint, currentPoint, TurfConstants.UNIT_METERS);

    log("Distance between updates: %s", distanceInMeters);

    // Average velocity = distance traveled over time
    long timeSinceLastValidUpdate = location.getTime() - getLastValidLocation().getTime();
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
      locations.add(location);
      return true;
    }

    // Not a valid update
    return false;
  }

  public Location retrieveProcessedLocation() {
    return averageValidLocations();
  }

  public void setLocationLoggingEnabled(boolean loggingEnabled) {
    this.loggingEnabled = loggingEnabled;
  }

  private Location averageValidLocations() {

    // TODO check if average is behind us don't count it

    double totalX = 0;
    double totalY = 0;
    double totalZ = 0;

    for (Location thisLoc : locations) {
      CartesianLocation thisCart = new CartesianLocation(thisLoc);
      totalX = totalX + thisCart.getxCoordinate();
      totalY = totalY + thisCart.getyCoordinate();
      totalZ = totalZ + thisCart.getzCoordinate();
    }

    double locationNumber = locations.size();
    double avgX = totalX / locationNumber;
    double avgY = totalY / locationNumber;
    double avgZ = totalZ / locationNumber;


    CartesianLocation avgCart = new CartesianLocation(new Location("null"));
    avgCart.setxCoordinate(avgX);
    avgCart.setyCoordinate(avgY);
    avgCart.setzCoordinate(avgZ);

    return avgCart.getGeodeticLocation();
  }

  private Location getLastValidLocation() {
    return locations.peek();
  }

  private void log(String message, Object... args) {
    if (loggingEnabled) {
      Timber.d(message, args);
    }
  }
}

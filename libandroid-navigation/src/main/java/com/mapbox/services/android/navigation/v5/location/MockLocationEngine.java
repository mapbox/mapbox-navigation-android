package com.mapbox.services.android.navigation.v5.location;

import android.location.Location;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import timber.log.Timber;

/**
 * Allows for mocking user location along a route. The route comes from the directions API and is instance of
 * {@link DirectionsRoute}. Once the route is passed in the mocking is automatically started. It goes step by step
 * within the directions route. This allows for long routing without memory issues. You have the option to either use
 * the default values for delay (1 second), speed (30m/h), and noisyGps (false) or you can pass in your own values when
 * constructing this object.
 *
 * @since 2.2.0
 */
public class MockLocationEngine extends LocationEngine {

  private static final int DEFAULT_DELAY = 1000; // 1s like most GPS intervals
  private static final int DEFAULT_SPEED = 30; // Miles per hour
  private static final boolean DEFAULT_NOISY_GPS = true;

  private Location lastLocation = new Location(MockLocationEngine.class.getSimpleName());
  private Location location;
  private List<LocationEngineListener> listeners;

  private boolean noisyGps;
  private int speed;
  private int delay;

  private List<Point> points = new ArrayList<>();
  private Runnable runnable;
  private Handler handler;

  private DirectionsRoute route;
  private int currentStep;
  private double distance;
  private int currentLeg;

  /*
   * Constructors
   */

  /**
   * Create a {@code MockLocationEngine} instance using the default parameters.
   *
   * @since 2.2.0
   */
  public MockLocationEngine() {
    listeners = new ArrayList<>();
    delay = DEFAULT_DELAY;
    speed = DEFAULT_SPEED;
    noisyGps = DEFAULT_NOISY_GPS;
  }

  /**
   * Create a {@code MockLocationEngine} instance with custom parameters.
   *
   * @param delay    the frequency in which the gps position is updated in milliseconds.
   * @param speed    the speed the user is traveling in miles per hour.
   * @param noisyGps true if you want the mock points to become noisy.
   * @since 2.2.0
   */
  public MockLocationEngine(int delay, int speed, boolean noisyGps) {
    listeners = new ArrayList<>();
    this.delay = delay;
    this.speed = speed;
    this.noisyGps = noisyGps;
  }

  /*
   * LocationEngine Methods
   */

  /**
   * Connect all the location listeners.
   *
   * @since 2.2.0
   */
  @Override
  public void activate() {
    for (LocationEngineListener listener : locationListeners) {
      listener.onConnected();
    }
  }


  /**
   * Stops mocking the user location along the route.
   *
   * @since 2.2.0
   */
  @Override
  public void deactivate() {
    Timber.d("Mock Location deactivated");
    if (handler != null && runnable != null) {
      handler.removeCallbacks(runnable);
    }
  }

  /**
   * While the {@code MockLocationEngine} is in use, you are always connected to it.
   *
   * @return true.
   * @since 2.2.0
   */
  @Override
  public boolean isConnected() {
    return true; // Always connected
  }

  /**
   * If the lastLocation is not null, this method will return the last location as expected. Otherwise, we return the
   * Mapbox DC office location.
   *
   * @return a {@link Location} which represents the last mock location.
   * @since 2.2.0
   */
  @Override
  @Nullable
  public Location getLastLocation() {
    if (lastLocation != null
      && lastLocation.getLongitude() != 0
      && lastLocation.getLatitude() != 0) {
      return lastLocation;
    } else {
      return null;
    }
  }

  public void setLastLocation(Point currentPosition) {
    lastLocation.setLongitude(currentPosition.longitude());
    lastLocation.setLatitude(currentPosition.latitude());
  }

  public boolean isNoisyGps() {
    return noisyGps;
  }

  public int getSpeed() {
    return speed;
  }

  public int getDelay() {
    return delay;
  }

  /**
   * Nothing needs to happen here since we are mocking the user location along a route.
   *
   * @since 2.2.0
   */
  @Override
  public void requestLocationUpdates() {

  }

  /**
   * Removes location updates for the LocationListener.
   *
   * @since 2.2.0
   */
  @Override
  public void removeLocationUpdates() {
    for (LocationEngineListener listener : listeners) {
      listeners.remove(listener);
    }
  }

  @Override
  public Type obtainType() {
    return Type.MOCK;
  }

  /*
   * Logic methods for getting the user points.
   */

  /**
   * Interpolates the route into even points along the route and adds these to the points list.
   *
   * @param lineString our route geometry.
   * @param distance   the distance you want to interpolate the line by, by default we calculate the distance using the
   *                   speed variable.
   * @since 2.2.0
   */
  private void sliceRoute(LineString lineString, double distance) {
    double distanceKm = TurfMeasurement.length(lineString, TurfConstants.UNIT_KILOMETERS);
    Timber.d("Route distance in km: %f", distanceKm);
    if (distanceKm <= 0) {
      return;
    }

    // Chop the line in small pieces
    for (double i = 0; i < distanceKm; i += distance) {
      Point point = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_KILOMETERS);
      points.add(point);
    }
  }

  /**
   * Emulate a noisy route using this method. Note that some points might not be noisy if the random value produced
   * equals 0.
   *
   * @since 2.2.0
   */
  private void addNoiseToRoute(double distance) {

    // End point will always match the given route (no noise will be added)
    for (int i = 0; i < points.size() - 1; i++) {

      double bearing = TurfMeasurement.bearing(points.get(i), points.get(i + 1));
      Random random = new Random();
      bearing = random.nextInt(15 - -15) + bearing;

      Point point = TurfMeasurement.destination(
        points.get(i), distance, bearing, TurfConstants.UNIT_KILOMETERS
      );
      points.set(i, point);
    }
  }

  /**
   * Converts the speed value to km/s and delay to seconds. Then the distance is calculated and returned.
   *
   * @return a double value representing the distance given a speed and time.
   * @since 2.2.0
   */
  private double calculateDistancePerSec() {
    //speed = 30 Miles/hour * 1.609344km/1mile * 1/60min * 1/60s
    double speed = (this.speed * 1.609344) / (60 * 60); // converted to km/s
    double time = delay / 1000; // convert to seconds
    return speed * time;
  }

  public void moveToLocation(Point point) {

    if (location == null) {
      return;
    }

    List<Point> pointList = new ArrayList<>();
    pointList.add(Point.fromLngLat(location.getLongitude(), location.getLatitude()));
    pointList.add(point);

    if (handler != null && runnable != null) {
      handler.removeCallbacks(runnable);
    }
    // Reset all variables
    handler = new Handler();
    points = new ArrayList<>();
    currentLeg = 0;
    currentStep = 0;

    // Calculate the distance which will always be consistent throughout the route.
    distance = calculateDistancePerSec();

    LineString route = LineString.fromLngLats(pointList);

    sliceRoute(route, distance);

    if (noisyGps) {
      addNoiseToRoute(distance);
    }

    runnable = new LocationUpdateRunnable();
    handler.postDelayed(runnable, delay);
  }

  /**
   * Use this method to pass in a route and start the mocking immediately.
   *
   * @param route a {@link DirectionsRoute} which you'd like to mock the user location on.
   * @since 2.2.0
   */
  public void setRoute(DirectionsRoute route) {
    this.route = route;

    if (handler != null && runnable != null) {
      handler.removeCallbacks(runnable);
    }
    // Reset all variables
    handler = new Handler();
    points = new ArrayList<>();
    currentLeg = 0;
    currentStep = 0;

    // Calculate the distance which will always be consistent throughout the route.
    distance = calculateDistancePerSec();

    calculateStepPoints();

    runnable = new LocationUpdateRunnable();
    handler.postDelayed(runnable, delay);
  }

  /**
   * Instead of calculating all the points found in the entire route geometry, we go step by step as needed until the
   * route in complete. This resolves a memory issue when long routes are being mocked.
   *
   * @since 2.2.0
   */
  private void calculateStepPoints() {
    LineString line = LineString.fromPolyline(
      route.legs().get(currentLeg).steps().get(currentStep).geometry(), Constants.PRECISION_6);

    increaseIndex();

    sliceRoute(line, distance);
    if (noisyGps) {
      addNoiseToRoute(distance);
    }
  }

  private void increaseIndex() {
    if (currentStep < route.legs().get(currentLeg).steps().size() - 1) {
      currentStep++;
    } else if (currentLeg < route.legs().size() - 1) {
      currentLeg++;
      currentStep = 0;
    }
  }

  /**
   * Here we build the new mock {@link Location} object and fill in as much information we can calculate.
   *
   * @param point taken from the points list, converts this to a {@link Location}.
   * @return a {@link Location} object with as much information filled in as possible.
   * @since 2.2.0
   */
  private Location mockLocation(Point point) {
    location = new Location(MockLocationEngine.class.getName());
    location.setLatitude(point.latitude());
    location.setLongitude(point.longitude());

    // Need to convert speed to meters/second as specified in Android's Location object documentation.
    float speedInMeterPerSec = (float) (((speed * 1.609344) * 1000) / (60 * 60));
    location.setSpeed(speedInMeterPerSec);

    if (points.size() >= 2) {
      double bearing = TurfMeasurement.bearing(point, points.get(1));
      Timber.v("Bearing value %f", bearing);
      location.setBearing((float) bearing);
    }

    location.setAccuracy(3f);
    location.setTime(System.currentTimeMillis());

    return location;
  }

  /**
   * A runnable which keeps the user location progressing along the route.
   *
   * @since 2.2.0
   */
  @SuppressWarnings( {"MissingPermission"})
  private class LocationUpdateRunnable implements Runnable {
    @Override
    public void run() {
      // Calculate the next steps points if the list becomes empty
      // so that the mock location continues along the route
      if (points.isEmpty()) {
        calculateStepPoints();
      }

      if (!points.isEmpty()) {
        // Notify of an update
        Location location = mockLocation(points.get(0));
        sendLocationUpdate(location);
        points.remove(0);
      } else {
        Location location = getLastLocation();
        sendLocationUpdate(location);
      }
      lastLocation = location;
      // Schedule the next update
      handler.postDelayed(this, delay);
    }

    private void sendLocationUpdate(Location location) {
      if (location == null) {
        return;
      }
      for (LocationEngineListener listener : locationListeners) {
        listener.onLocationChanged(location);
      }
    }
  }
}
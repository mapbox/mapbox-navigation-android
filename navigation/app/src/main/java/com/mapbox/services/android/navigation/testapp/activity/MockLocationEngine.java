package com.mapbox.services.android.navigation.testapp.activity;

import android.location.Location;
import android.os.Handler;

import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfException;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.geojson.LineString;
import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import timber.log.Timber;

import static com.mapbox.services.Constants.PRECISION_6;

/**
 * Allows for mocking user location along a route. The route comes from the directions API and is instance of
 * {@link DirectionsRoute}. Once the route is passed in the mocking is automatically started. It goes step by step
 * within the directions route. This allows for long routing without memory issues. You have the option to either use
 * the default values for delay (1 second), speed (30m/h), and noisyGps (false) or you can pass in your own values when
 * constructing this object.
 *
 * @since 2.0.0
 */
public class MockLocationEngine extends LocationEngine {

  private static final int DEFAULT_DELAY = 1000; // 1s like most GPS intervals
  private static final int DEFAULT_SPEED = 30; // Miles per hour
  private static final boolean DEFAULT_NOISY_GPS = true;

  private Location lastLocation = new Location(MockLocationEngine.class.getSimpleName());
  private List<LocationEngineListener> listeners;

  private boolean noisyGps = DEFAULT_NOISY_GPS;
  private int speed = DEFAULT_SPEED;
  private int delay = DEFAULT_DELAY;

  private List<Position> positions = new ArrayList<>();
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
   * @since 2.0.0
   */
  public MockLocationEngine() {
    listeners = new ArrayList<>();
  }

  /**
   * Create a {@code MockLocationEngine} instance with custom parameters.
   *
   * @param delay    the frequency in which the gps position is updated in milliseconds.
   * @param speed    the speed the user is traveling in miles per hour.
   * @param noisyGps true if you want the mock positions to become noisy.
   * @since 2.0.0
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
   * @since 2.0.0
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
   * @since 2.0.0
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
   * @since 2.0.0
   */
  @Override
  public Location getLastLocation() {
    if (lastLocation.getLongitude() != 0 && lastLocation.getLatitude() != 0) {
      return lastLocation;
    } else {
      lastLocation.setLatitude(41.8529);
      lastLocation.setLongitude(-87.6900);
      return lastLocation;
    }
  }

  /**
   * Nothing needs to happen here since we are mocking the user location along a route.
   *
   * @since 2.0.0
   */
  @Override
  public void requestLocationUpdates() {

  }

  /**
   * Removes location updates for the LocationListener.
   *
   * @since 2.0.0
   */
  @Override
  public void removeLocationUpdates() {
    for (LocationEngineListener listener : listeners) {
      listeners.remove(listener);
    }
  }

  /*
   * Logic methods for getting the user positions.
   */

  /**
   * Interpolates the route into even positions along the route and adds these to the positions list.
   *
   * @param lineString our route geometry.
   * @param distance   the distance you want to interpolate the line by, by default we calculate the distance using the
   *                   speed variable.
   * @since 2.0.0
   */
  private void sliceRoute(LineString lineString, double distance) {
    double distanceKm = TurfMeasurement.lineDistance(lineString, TurfConstants.UNIT_KILOMETERS);
    Timber.d("Route distance in km: %f", distanceKm);
    if (distanceKm <= 0) {
      return;
    }

    // Chop the line in small pieces
    for (double i = 0; i < distanceKm; i += distance) {
      Position position = TurfMeasurement.along(lineString, i, TurfConstants.UNIT_KILOMETERS).getCoordinates();
      positions.add(position);
    }
  }

  /**
   * Emulate a noisy route using this method. Note that some points might not be noisy if the random value produced
   * equals 0.
   *
   * @throws TurfException occurs when turf fails to calculate either bearing or destination.
   * @since 2.0.0
   */
  private void addNoiseToRoute(double distance) throws TurfException {

    // End point will always match the given route (no noise will be added)
    for (int i = 0; i < positions.size() - 1; i++) {

      double bearing = TurfMeasurement.bearing(positions.get(i), positions.get(i + 1));
      Random random = new Random();
      bearing = random.nextInt(15 - -15) + bearing;

      Position position = TurfMeasurement.destination(
        positions.get(i), distance, bearing, TurfConstants.UNIT_KILOMETERS
      );
      positions.set(i, position);
    }
  }

  /**
   * Converts the speed value to km/s and delay to seconds. Then the distance is calculated and returned.
   *
   * @return a double value representing the distance given a speed and time.
   * @since 2.0.0
   */
  private double calculateDistancePerSec() {
    //speed = 30 Miles/hour * 1.609344km/1mile * 1/60min * 1/60s
    double speed = (this.speed * 1.609344) / (60 * 60); // converted to km/s
    double time = delay / 1000; // convert to seconds
    return speed * time;
  }

  public void moveToLocation(Position position) {
    List<Position> positionList = new ArrayList<>();
    positionList.add(Position.fromLngLat(lastLocation.getLongitude(), lastLocation.getLatitude()));
    positionList.add(position);

    LineString route = LineString.fromCoordinates(positionList);

    if (handler != null && runnable != null) {
      handler.removeCallbacks(runnable);
    }
    // Reset all variables
    handler = new Handler();
    positions = new ArrayList<>();
    currentLeg = 0;
    currentStep = 0;

    // Calculate the distance which will always be consistent throughout the route.
    distance = calculateDistancePerSec();

    sliceRoute(route, distance);
    if (noisyGps) {
      addNoiseToRoute(distance);
    }

    handler.postDelayed(runnable = new LocationUpdateRunnable(), delay);
  }

  /**
   * Use this method to pass in a route and start the mocking immediately.
   *
   * @param route a {@link DirectionsRoute} which you'd like to mock the user location on.
   * @since 2.0.0
   */
  public void setRoute(DirectionsRoute route) {
    this.route = route;

    if (handler != null && runnable != null) {
      handler.removeCallbacks(runnable);
    }
    // Reset all variables
    handler = new Handler();
    positions = new ArrayList<>();
    currentLeg = 0;
    currentStep = 0;

    // Calculate the distance which will always be consistent throughout the route.
    distance = calculateDistancePerSec();

    calculateStepPoints();

    handler.postDelayed(runnable = new LocationUpdateRunnable(), delay);
  }

  /**
   * Instead of calculating all the points found in the entire route geometry, we go step by step as needed until the
   * route in complete. This resolves a memory issue when long routes are being mocked.
   *
   * @throws TurfException occurs when turf fails to calculate either bearing or distance.
   * @since 2.0.0
   */
  private void calculateStepPoints() {
    LineString line = LineString.fromPolyline(
      route.getLegs().get(currentLeg)
        .getSteps().get(currentStep).getGeometry(), PRECISION_6);

    if (currentStep < route.getLegs().get(currentLeg).getSteps().size() - 1) {
      currentStep++;
    } else if (currentLeg < route.getLegs().size() - 1) {
      currentLeg++;
    }

    sliceRoute(line, distance);
    if (noisyGps) {
      addNoiseToRoute(distance);
    }
  }

  /**
   * Here we build the new mock {@link Location} object and fill in as much information we can calculate.
   *
   * @param position taken from the positions list, converts this to a {@link Location}.
   * @return a {@link Location} object with as much information filled in as possible.
   * @since 2.0.0
   */
  private Location mockLocation(Position position) {
    lastLocation = new Location(MockLocationEngine.class.getName());
    lastLocation.setLatitude(position.getLatitude());
    lastLocation.setLongitude(position.getLongitude());

    // Need to convert speed to meters/second as specified in Android's Location object documentation.
    float speedInMeterPerSec = (float) (((speed * 1.609344) * 1000) / (60 * 60));
    lastLocation.setSpeed(speedInMeterPerSec);

    if (positions.size() >= 2) {
      double bearing = TurfMeasurement.bearing(position, positions.get(1));
      Timber.v("Bearing value %f", bearing);
      lastLocation.setBearing((float) bearing);
    }

    return lastLocation;
  }

  /**
   * A runnable which keeps the user location progressing along the route.
   *
   * @since 2.0.0
   */
  @SuppressWarnings( {"MissingPermission"})
  private class LocationUpdateRunnable implements Runnable {
    @Override
    public void run() {
      if (positions.size() <= 5) {
        calculateStepPoints();
      }

      Timber.v("current position size representing %d", positions.size());
      if (positions.size() > 0) {
        // Notify of an update
        Location location = mockLocation(positions.get(0));
        for (LocationEngineListener listener : locationListeners) {
          listener.onLocationChanged(location);
        }
        positions.remove(0);
      } else {
        Location location = getLastLocation();
        for (LocationEngineListener listener : locationListeners) {
          listener.onLocationChanged(location);
        }
      }
      // Schedule the next update
      handler.postDelayed(this, delay);
    }
  }
}
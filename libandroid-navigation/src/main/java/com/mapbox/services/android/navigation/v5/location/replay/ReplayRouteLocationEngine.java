package com.mapbox.services.android.navigation.v5.location.replay;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;


public class ReplayRouteLocationEngine extends LocationEngine implements Runnable {

  private static final int HEAD = 0;
  private static final int MOCKED_POINTS_LEFT_THRESHOLD = 5;
  private static final int ONE_SECOND_IN_MILLISECONDS = 1000;
  private static final int FORTY_FIVE_KM_PER_HOUR = 45;
  private static final int DEFAULT_SPEED = FORTY_FIVE_KM_PER_HOUR;
  private static final int ONE_SECOND = 1;
  private static final int DEFAULT_DELAY = ONE_SECOND;
  private static final int DO_NOT_DELAY = 0;
  private static final int ZERO = 0;
  private static final String SPEED_MUST_BE_GREATER_THAN_ZERO_KM_H = "Speed must be greater than 0 km/h.";
  private static final String DELAY_MUST_BE_GREATER_THAN_ZERO_SECONDS = "Delay must be greater than 0 seconds.";
  private static final String REPLAY_ROUTE = "ReplayRouteLocation";
  private ReplayRouteLocationConverter converter;
  private int speed = DEFAULT_SPEED;
  private int delay = DEFAULT_DELAY;
  private Handler handler;
  private List<Location> mockedLocations;
  private ReplayLocationDispatcher dispatcher;
  private Location lastLocation = null;
  private final ReplayLocationListener replayLocationListener = new ReplayLocationListener() {
    @Override
    public void onLocationReplay(Location location) {
      for (LocationEngineListener listener : locationListeners) {
        listener.onLocationChanged(location);
      }
      lastLocation = location;
      if (!mockedLocations.isEmpty()) {
        mockedLocations.remove(HEAD);
      }
    }
  };

  public ReplayRouteLocationEngine() {
    this.handler = new Handler();
  }

  @SuppressLint("MissingPermission")
  public void assign(DirectionsRoute route) {
    start(route);
  }

  @SuppressLint("MissingPermission")
  public void moveTo(Point point) {
    Location lastLocation = getLastLocation();
    if (lastLocation == null) {
      return;
    }

    startRoute(point, lastLocation);
  }

  public void assignLastLocation(Point currentPosition) {
    initializeLastLocation();
    lastLocation.setLongitude(currentPosition.longitude());
    lastLocation.setLatitude(currentPosition.latitude());
  }

  public void updateSpeed(int customSpeedInKmPerHour) {
    if (customSpeedInKmPerHour <= 0) {
      throw new IllegalArgumentException(SPEED_MUST_BE_GREATER_THAN_ZERO_KM_H);
    }
    this.speed = customSpeedInKmPerHour;
  }

  public void updateDelay(int customDelayInSeconds) {
    if (customDelayInSeconds <= 0) {
      throw new IllegalArgumentException(DELAY_MUST_BE_GREATER_THAN_ZERO_SECONDS);
    }
    this.delay = customDelayInSeconds;
  }

  @Override
  public void run() {
    List<Location> nextMockedLocations = converter.toLocations();
    if (nextMockedLocations.isEmpty()) {
      handler.removeCallbacks(this);
      return;
    }
    dispatcher.add(nextMockedLocations);
    mockedLocations.addAll(nextMockedLocations);
    scheduleNextDispatch();
  }

  /**
   * Connect all the location listeners.
   */
  @Override
  public void activate() {
    for (LocationEngineListener listener : locationListeners) {
      listener.onConnected();
    }
  }

  @Override
  public void deactivate() {
    if (dispatcher != null) {
      dispatcher.stop();
    }
    handler.removeCallbacks(this);
  }

  /**
   * While the {@link ReplayRouteLocationEngine} is in use, you are always connected to it.
   *
   * @return true.
   */
  @Override
  public boolean isConnected() {
    return true;
  }

  @SuppressLint("MissingPermission")
  @Override
  @Nullable
  public Location getLastLocation() {
    return lastLocation;
  }

  /**
   * Nothing needs to happen here since we are mocking the user location along a route.
   */
  @Override
  public void requestLocationUpdates() {

  }

  /**
   * Removes location updates for the LocationListener.
   */
  @Override
  public void removeLocationUpdates() {
    for (LocationEngineListener listener : locationListeners) {
      locationListeners.remove(listener);
    }
    if (dispatcher != null) {
      dispatcher.removeReplayLocationListener(replayLocationListener);
    }
  }

  @Override
  public Type obtainType() {
    return Type.MOCK;
  }

  private void start(DirectionsRoute route) {
    handler.removeCallbacks(this);
    converter = new ReplayRouteLocationConverter(route, speed, delay);
    converter.initializeTime();
    mockedLocations = converter.toLocations();
    dispatcher = obtainDispatcher();
    dispatcher.run();
    scheduleNextDispatch();
  }

  private ReplayLocationDispatcher obtainDispatcher() {
    if (dispatcher != null) {
      dispatcher.stop();
      dispatcher.removeReplayLocationListener(replayLocationListener);
    }
    dispatcher = new ReplayLocationDispatcher(mockedLocations);
    dispatcher.addReplayLocationListener(replayLocationListener);

    return dispatcher;
  }

  private void startRoute(Point point, Location lastLocation) {
    handler.removeCallbacks(this);
    converter.updateSpeed(speed);
    converter.updateDelay(delay);
    converter.initializeTime();
    LineString route = obtainRoute(point, lastLocation);
    mockedLocations = converter.calculateMockLocations(converter.sliceRoute(route));
    dispatcher = obtainDispatcher();
    dispatcher.run();
  }

  @NonNull
  private LineString obtainRoute(Point point, Location lastLocation) {
    List<Point> pointList = new ArrayList<>();
    pointList.add(Point.fromLngLat(lastLocation.getLongitude(), lastLocation.getLatitude()));
    pointList.add(point);
    return LineString.fromLngLats(pointList);
  }

  private void scheduleNextDispatch() {
    int currentMockedPoints = mockedLocations.size();
    if (currentMockedPoints == ZERO) {
      handler.postDelayed(this, DO_NOT_DELAY);
    } else if (currentMockedPoints <= MOCKED_POINTS_LEFT_THRESHOLD) {
      handler.postDelayed(this, ONE_SECOND_IN_MILLISECONDS);
    } else {
      handler.postDelayed(this, (currentMockedPoints - MOCKED_POINTS_LEFT_THRESHOLD) * ONE_SECOND_IN_MILLISECONDS);
    }
  }

  private void initializeLastLocation() {
    if (lastLocation == null) {
      lastLocation = new Location(REPLAY_ROUTE);
    }
  }
}

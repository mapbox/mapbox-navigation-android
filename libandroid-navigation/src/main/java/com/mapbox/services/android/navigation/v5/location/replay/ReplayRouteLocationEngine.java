package com.mapbox.services.android.navigation.v5.location.replay;

import android.app.PendingIntent;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;


public class ReplayRouteLocationEngine implements LocationEngine, Runnable {

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
  private ReplayRouteLocationListener replayLocationListener;
  private Location lastLocation = null;
  private DirectionsRoute route = null;
  private Point point = null;

  public ReplayRouteLocationEngine() {
    this.handler = new Handler();
  }

  public void assign(DirectionsRoute route) {
    this.route = route;
    this.point = null;
  }

  public void moveTo(Point point) {
    this.point = point;
    this.route = null;
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
      if (converter.isMultiLegRoute()) {
        nextMockedLocations = converter.toLocations();
      } else {
        handler.removeCallbacks(this);
        return;
      }
    }
    dispatcher.add(nextMockedLocations);
    mockedLocations.addAll(nextMockedLocations);
    scheduleNextDispatch();
  }

  @Override
  public void getLastLocation(@NonNull LocationEngineCallback<LocationEngineResult> callback) throws SecurityException {
    if (lastLocation == null) {
      callback.onFailure(new Exception("Last location can't be null"));
      return;
    }
    callback.onSuccess(LocationEngineResult.create(lastLocation));
  }

  @Override
  public void requestLocationUpdates(@NonNull LocationEngineRequest request,
                                     @NonNull LocationEngineCallback<LocationEngineResult> callback,
                                     @Nullable Looper looper) throws SecurityException {
    beginReplayWith(callback);
  }

  @Override
  public void requestLocationUpdates(@NonNull LocationEngineRequest request,
                                     PendingIntent pendingIntent) throws SecurityException {
    Timber.e("ReplayEngine does not support PendingIntent.");
  }

  @Override
  public void removeLocationUpdates(@NonNull LocationEngineCallback<LocationEngineResult> callback) {
    deactivate();
  }

  @Override
  public void removeLocationUpdates(PendingIntent pendingIntent) {
    Timber.e("ReplayEngine does not support PendingIntent.");
  }

  void updateLastLocation(Location lastLocation) {
    this.lastLocation = lastLocation;
  }

  void removeLastMockedLocation() {
    if (!mockedLocations.isEmpty()) {
      mockedLocations.remove(HEAD);
    }
  }

  private void deactivate() {
    if (dispatcher != null) {
      dispatcher.stop();
    }
    handler.removeCallbacks(this);
  }

  private void start(DirectionsRoute route, LocationEngineCallback<LocationEngineResult> callback) {
    handler.removeCallbacks(this);
    converter = new ReplayRouteLocationConverter(route, speed, delay);
    converter.initializeTime();
    mockedLocations = converter.toLocations();
    dispatcher = obtainDispatcher(callback);
    dispatcher.run();
    scheduleNextDispatch();
  }

  private ReplayLocationDispatcher obtainDispatcher(LocationEngineCallback<LocationEngineResult> callback) {
    if (dispatcher != null && replayLocationListener != null) {
      dispatcher.stop();
      dispatcher.removeReplayLocationListener(replayLocationListener);
    }
    dispatcher = new ReplayLocationDispatcher(mockedLocations);
    replayLocationListener = new ReplayRouteLocationListener(this, callback);
    dispatcher.addReplayLocationListener(replayLocationListener);

    return dispatcher;
  }

  private void startRoute(Point point, Location lastLocation, LocationEngineCallback<LocationEngineResult> callback) {
    handler.removeCallbacks(this);
    converter.updateSpeed(speed);
    converter.updateDelay(delay);
    converter.initializeTime();
    LineString route = obtainRoute(point, lastLocation);
    mockedLocations = converter.calculateMockLocations(converter.sliceRoute(route));
    dispatcher = obtainDispatcher(callback);
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

  private void beginReplayWith(@NonNull LocationEngineCallback<LocationEngineResult> callback) {
    if (route != null) {
      start(route, callback);
    } else if (point != null) {
      if (lastLocation == null) {
        callback.onFailure(new Exception("Cannot move to point without last location assigned."));
        return;
      }
      startRoute(point, lastLocation, callback);
    } else {
      callback.onFailure(new Exception("No route found to replay."));
    }
  }

  private void initializeLastLocation() {
    if (lastLocation == null) {
      lastLocation = new Location(REPLAY_ROUTE);
    }
  }
}

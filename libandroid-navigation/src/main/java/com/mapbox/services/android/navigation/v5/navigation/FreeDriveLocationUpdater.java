package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.geojson.Point;
import com.mapbox.navigator.FixLocation;
import com.mapbox.navigator.NavigationStatus;
import com.mapbox.services.android.navigation.v5.internal.navigation.MapboxNavigator;
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationEventDispatcher;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

class FreeDriveLocationUpdater {

  private final LocationEngine locationEngine;
  private final LocationEngineRequest locationEngineRequest;
  private final NavigationEventDispatcher navigationEventDispatcher;
  private final MapboxNavigator mapboxNavigator;
  private final ScheduledExecutorService executorService;
  private final LocationEngineCallback<LocationEngineResult> callback = new CurrentLocationEngineCallback(this);
  private ScheduledFuture future;
  private Location rawLocation;

  FreeDriveLocationUpdater(@NonNull LocationEngine locationEngine,
                           @NonNull LocationEngineRequest locationEngineRequest,
                           @NonNull NavigationEventDispatcher navigationEventDispatcher,
                           @NonNull MapboxNavigator mapboxNavigator,
                           @NonNull ScheduledExecutorService executorService) {
    this.locationEngine = locationEngine;
    this.locationEngineRequest = locationEngineRequest;
    this.navigationEventDispatcher = navigationEventDispatcher;
    this.mapboxNavigator = mapboxNavigator;
    this.executorService = executorService;
  }

  void configure(@NonNull File path, @NonNull String version, @NonNull String host,
                 @NonNull String accessToken,
                 @NonNull OnOfflineTilesConfiguredCallback onOfflineTilesConfiguredCallback) {
    OfflineNavigator offlineNavigator =
        new OfflineNavigator(mapboxNavigator.getNavigator(), version, host, accessToken);
    String tilePath = new File(path, version).getAbsolutePath();
    offlineNavigator.configure(tilePath, onOfflineTilesConfiguredCallback);
  }

  void start() {
    if (future == null) {
      locationEngine.requestLocationUpdates(locationEngineRequest, callback, null);
      final Handler handler = new Handler(Looper.getMainLooper());
      future = executorService.scheduleAtFixedRate(new Runnable() {
        @Override
        public void run() {
          if (rawLocation != null) {
            final Location enhancedLocation = getLocation(new Date(), 0, rawLocation);
            handler.post(new Runnable() {
              @Override
              public void run() {
                navigationEventDispatcher.onEnhancedLocationUpdate(enhancedLocation);
              }
            });
          }
        }
      }, 1500, 1000, TimeUnit.MILLISECONDS);
    }
  }

  void stop() {
    if (future != null) {
      stopLocationUpdates();
    }
  }

  void kill() {
    if (future != null) {
      stopLocationUpdates();
      executorService.shutdown();
    }
  }

  private void stopLocationUpdates() {
    locationEngine.removeLocationUpdates(callback);
    future.cancel(false);
    future = null;
  }

  private Location getLocation(Date date, long lagMillis, Location rawLocation) {
    NavigationStatus status = mapboxNavigator.retrieveStatus(date, lagMillis);
    return getMapMatchedLocation(status, rawLocation);
  }

  private Location getMapMatchedLocation(NavigationStatus status, Location fallbackLocation) {
    Location snappedLocation = new Location(fallbackLocation);
    snappedLocation.setProvider("enhanced");
    FixLocation fixLocation = status.getLocation();
    Point coordinate = fixLocation.getCoordinate();
    snappedLocation.setLatitude(coordinate.latitude());
    snappedLocation.setLongitude(coordinate.longitude());
    if (fixLocation.getBearing() != null) {
      snappedLocation.setBearing(fixLocation.getBearing());
    }
    snappedLocation.setTime(fixLocation.getTime().getTime());
    return snappedLocation;
  }

  private void onLocationChanged(Location location) {
    if (location != null) {
      rawLocation = location;
      mapboxNavigator.updateLocation(location);
    }
  }

  static class CurrentLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

    private final WeakReference<FreeDriveLocationUpdater> updaterWeakReference;

    CurrentLocationEngineCallback(FreeDriveLocationUpdater locationUpdater) {
      this.updaterWeakReference = new WeakReference<>(locationUpdater);
    }

    @Override
    public void onSuccess(LocationEngineResult result) {
      FreeDriveLocationUpdater locationUpdater = updaterWeakReference.get();
      if (locationUpdater != null) {
        Location location = result.getLastLocation();
        locationUpdater.onLocationChanged(location);
      }
    }

    @Override
    public void onFailure(@NonNull Exception exception) {
      Timber.e(exception);
    }
  }
}

package com.mapbox.services.android.navigation.v5.navigation;

import android.app.PendingIntent;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.geojson.Point;
import com.mapbox.navigator.FixLocation;
import com.mapbox.navigator.NavigationStatus;
import com.mapbox.services.android.navigation.v5.internal.navigation.MapboxNavigator;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

class EnhancedLocationEngineDecorator implements LocationEngine {
  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 500;
  private final LocationEngine locationEngine;
  private final MapboxNavigator mapboxNavigator;
  private final LocationEngineCallback<LocationEngineResult> callback = new CurrentLocationEngineCallback(this);
  private ScheduledFuture future;
  private final ScheduledExecutorService executorService;
  private Location rawLocation;
  private LocationEngineCallback<LocationEngineResult> originalCallback;
  private Looper originalLooper;
  private boolean isEnhancedInitialized = false;
  private AtomicBoolean isConfigured = new AtomicBoolean(false);

  EnhancedLocationEngineDecorator(@NonNull LocationEngine locationEngine,
                                  @NonNull MapboxNavigator mapboxNavigator,
                                  @NonNull ScheduledExecutorService executorService) {
    this.locationEngine = locationEngine;
    this.mapboxNavigator = mapboxNavigator;
    this.executorService = executorService;
  }

  @Override
  public void getLastLocation(@NonNull LocationEngineCallback<LocationEngineResult> callback) throws SecurityException {
    locationEngine.getLastLocation(callback);
  }

  @Override
  public void requestLocationUpdates(@NonNull LocationEngineRequest request,
                                     @NonNull final LocationEngineCallback<LocationEngineResult> callback,
                                     @Nullable Looper looper) throws SecurityException {
    if (!isEnhancedInitialized) {
      isEnhancedInitialized = true;
      originalCallback = callback;
      originalLooper = looper;
      onFreeDrive();
    } else {
      locationEngine.requestLocationUpdates(request, callback, looper);
    }
  }

  @Override
  public void requestLocationUpdates(@NonNull LocationEngineRequest request,
                                     PendingIntent pendingIntent) throws SecurityException {
    locationEngine.requestLocationUpdates(request, pendingIntent);
  }

  @Override
  public void removeLocationUpdates(@NonNull LocationEngineCallback<LocationEngineResult> callback) {
    locationEngine.removeLocationUpdates(callback);
  }

  @Override
  public void removeLocationUpdates(PendingIntent pendingIntent) {
    locationEngine.removeLocationUpdates(pendingIntent);
  }

  void onActiveGuidance() {
    if (isConfigured.get()) {
      removeLocationUpdates(callback);
      if (future != null) {
        future.cancel(false);
        future = null;
      }
    }
  }

  void onFreeDrive() {
    requestLocationUpdates(obtainLocationEngineRequest(), callback, null);
    final Handler handler = new Handler(originalLooper.getMainLooper());
    future = executorService.scheduleAtFixedRate(new Runnable() {
        @Override
        public void run() {
        if (rawLocation != null) {
          final Location enhancedLocation = getLocation(new Date(), 0, rawLocation);
          handler.post(new Runnable() {
              @Override
              public void run() {
              originalCallback.onSuccess(LocationEngineResult.create(enhancedLocation));
            }
          });
        }
      }
    }, 1500, 1000, TimeUnit.MILLISECONDS);
  }

  void removeOriginalLocationUpdates() {
    if (isConfigured.get()) {
      if (originalCallback != null) {
        removeLocationUpdates(originalCallback);
        originalCallback = null;
      }
      isEnhancedInitialized = false;
    }
  }

  void onNavigationKilled() {
    if (isConfigured.get()) {
      if (future != null) {
        removeLocationUpdates(callback);
        future.cancel(false);
        future = null;
        executorService.shutdown();
        removeOriginalLocationUpdates();
      }
    }
  }

  void configure(@NonNull File path, @NonNull String version, @NonNull String host,
                 @NonNull String accessToken) {
    OfflineNavigator offlineNavigator =
      new OfflineNavigator(mapboxNavigator.getNavigator(), version, host, accessToken);
    String tilePath = new File(path, version).getAbsolutePath();
    offlineNavigator.configure(tilePath, new OnOfflineTilesConfiguredCallback() {
      @Override
      public void onConfigured(int numberOfTiles) {
        Timber.d("DEBUG: onConfigured %d", numberOfTiles);
        isConfigured.set(true);
      }

      @Override
      public void onConfigurationError(@NonNull OfflineError error) {
        Timber.d("DEBUG: onConfigurationError %s", error.getMessage());
        isConfigured.set(false);
      }
    });
  }

  // TODO Remove when MapboxNavigation is refactored - added to get current tests to pass
  LocationEngine getLocationEngine() {
    return locationEngine;
  }

  @NonNull
  private static LocationEngineRequest obtainLocationEngineRequest() {
    return new LocationEngineRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
      .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
      .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
      .build();
  }

  private static Location getSnappedLocation(NavigationStatus status, Location fallbackLocation) {
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

  private Location getLocation(Date date, long lagMillis, Location fallbackLocation) {
    if (isConfigured.get()) {
      NavigationStatus status = mapboxNavigator.retrieveStatus(date, lagMillis);
      return getSnappedLocation(status, fallbackLocation);
    }
    return fallbackLocation;
  }

  private static final class CurrentLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {
    private final WeakReference<EnhancedLocationEngineDecorator> locationEngine;

    CurrentLocationEngineCallback(@NonNull EnhancedLocationEngineDecorator locationEngine) {
      this.locationEngine = new WeakReference<>(locationEngine);
    }

    @Override
    public void onSuccess(LocationEngineResult result) {
      EnhancedLocationEngineDecorator enhancedLocationEngine = locationEngine.get();
      if (enhancedLocationEngine != null) {
        Location location = result.getLastLocation();
        enhancedLocationEngine.onLocationChanged(location);
      }
    }

    @Override
    public void onFailure(@NonNull Exception exception) {
      Timber.e(exception);
    }
  }
}

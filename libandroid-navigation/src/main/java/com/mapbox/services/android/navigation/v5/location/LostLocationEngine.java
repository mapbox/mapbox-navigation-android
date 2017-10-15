package com.mapbox.services.android.navigation.v5.location;

import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;

import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapzen.android.lost.api.LocationListener;
import com.mapzen.android.lost.api.LocationRequest;
import com.mapzen.android.lost.api.LocationServices;
import com.mapzen.android.lost.api.LostApiClient;

import java.lang.ref.WeakReference;

/**
 * Sample LocationEngine using the Open Source Lost library
 */
public class LostLocationEngine extends LocationEngine implements LocationListener {

  private static LocationEngine instance;

  private WeakReference<Context> context;
  private LostApiClient lostApiClient;

  public LostLocationEngine(Context context) {
    super();
    this.context = new WeakReference<>(context);
    lostApiClient = new LostApiClient.Builder(this.context.get()).build();
  }

  public static synchronized LocationEngine getLocationEngine(Context context) {
    if (instance == null) {
      instance = new LostLocationEngine(context.getApplicationContext());
    }

    return instance;
  }

  /**
   * Activate the location engine which will connect whichever location provider you are using. You'll need to call
   * this before requesting user location updates using {@link LocationEngine#requestLocationUpdates()}.
   */
  @Override
  public void activate() {
    if (!lostApiClient.isConnected()) {
      lostApiClient.connect();
    }
    for (LocationEngineListener listener : locationListeners) {
      listener.onConnected();
    }
  }

  /**
   * Disconnect the location engine which is useful when you no longer need location updates or requesting the users
   * {@link LocationEngine#getLastLocation()}. Before deactivating, you'll need to stop request user location updates
   * using {@link LocationEngine#removeLocationUpdates()}.
   */
  @Override
  public void deactivate() {
    if (lostApiClient.isConnected()) {
      lostApiClient.disconnect();
    }
  }

  /**
   * Check if your location provider has been activated/connected. This is mainly used internally but is also useful in
   * the rare case when you'd like to know if your location engine is connected or not.
   *
   * @return boolean true if the location engine has been activated/connected, else false.
   */
  @Override
  public boolean isConnected() {
    return lostApiClient.isConnected();
  }

  /**
   * Returns the Last known location if the location provider is connected and location permissions are granted.
   *
   * @return the last known location
   */
  @Override
  @Nullable
  public Location getLastLocation() {
    if (lostApiClient.isConnected()) {
      //noinspection MissingPermission
      return LocationServices.FusedLocationApi.getLastLocation();
    }
    return null;
  }

  /**
   * Request location updates to the location provider.
   */
  @Override
  public void requestLocationUpdates() {
    LocationRequest request = LocationRequest.create();

    if (interval != null) {
      request.setInterval(interval);
    }
    if (fastestInterval != null) {
      request.setFastestInterval(fastestInterval);
    }
    if (smallestDisplacement != null) {
      request.setSmallestDisplacement(smallestDisplacement);
    }

    // Priority matching is straightforward
    if (priority == LocationEnginePriority.NO_POWER) {
      request.setPriority(LocationRequest.PRIORITY_NO_POWER);
    } else if (priority == LocationEnginePriority.LOW_POWER) {
      request.setPriority(LocationRequest.PRIORITY_LOW_POWER);
    } else if (priority == LocationEnginePriority.BALANCED_POWER_ACCURACY) {
      request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    } else if (priority == LocationEnginePriority.HIGH_ACCURACY) {
      request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    if (lostApiClient.isConnected()) {
      //noinspection MissingPermission
      LocationServices.FusedLocationApi.requestLocationUpdates(request, this);
    }
  }

  @Override
  public Type obtainType() {
    return Type.LOST;
  }

  /**
   * Dismiss ongoing location update to the location provider.
   */
  @Override
  public void removeLocationUpdates() {
    if (lostApiClient.isConnected()) {
      LocationServices.FusedLocationApi.removeLocationUpdates(this);
    }
  }

  /**
   * Invoked when the Location has changed.
   *
   * @param location the new location
   */
  @Override
  public void onLocationChanged(Location location) {
    for (LocationEngineListener listener : locationListeners) {
      listener.onLocationChanged(location);
    }
  }
}
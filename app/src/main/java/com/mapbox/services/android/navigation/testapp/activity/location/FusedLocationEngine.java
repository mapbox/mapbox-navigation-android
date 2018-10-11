package com.mapbox.services.android.navigation.testapp.activity.location;

import android.app.Activity;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;

import java.util.Date;

import timber.log.Timber;

public class FusedLocationEngine extends LocationEngine {

  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 500;

  private FusedLocationProviderClient fusedLocationClient;
  private SettingsClient settingsClient;
  private LocationRequest locationRequest;
  private LocationSettingsRequest locationSettingsRequest;
  private boolean isConnected = false;
  private boolean receivingUpdates = false;
  private Location lastLocation = null;
  private final ForwardingLocationCallback forwardingCallback = new ForwardingLocationCallback(this);

  public FusedLocationEngine(@NonNull Activity activity) {
    super();
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
    settingsClient = LocationServices.getSettingsClient(activity);
  }

  @Override
  public void activate() {
    if (!isConnected) {
      startLocationUpdates();
    }
  }

  @Override
  public void deactivate() {
    stopLocationUpdates();
    isConnected = false;
  }

  @Override
  public boolean isConnected() {
    return isConnected;
  }

  @Override
  @SuppressWarnings("MissingPermission")
  public Location getLastLocation() {
    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
      location.setTime((new Date()).getTime()); //match system clock
      lastLocation = location;
    });
    return lastLocation;
  }

  @Override
  @SuppressWarnings( {"MissingPermission"})
  public void requestLocationUpdates() {
    if (isConnected && !receivingUpdates) {
      fusedLocationClient.requestLocationUpdates(locationRequest, forwardingCallback, Looper.myLooper())
        .addOnSuccessListener(aVoid -> receivingUpdates = true);
    }
  }

  @Override
  public void removeLocationUpdates() {
    stopLocationUpdates();
  }

  @Override
  public Type obtainType() {
    return Type.GOOGLE_PLAY_SERVICES;
  }

  void notifyListenersOnLocationChanged(Location location) {
    location.setTime((new Date()).getTime()); //match system clock
    for (LocationEngineListener listener : locationListeners) {
      listener.onLocationChanged(location);
    }
  }

  private void createLocationRequest() {
    locationRequest = new LocationRequest();
    locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
    locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
  }

  private void buildLocationSettingsRequest() {
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(locationRequest);
    locationSettingsRequest = builder.build();
  }

  private void startLocationUpdates() {
    createLocationRequest();
    buildLocationSettingsRequest();
    settingsClient.checkLocationSettings(locationSettingsRequest)
      .addOnSuccessListener(locationSettingsResponse -> {
        isConnected = true;
        notifyListenersOnConnected();
      })
      .addOnFailureListener(exception -> {
        isConnected = false;
        int statusCode = ((ApiException) exception).getStatusCode();
        switch (statusCode) {
          case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
            Timber.e("Location settings are not satisfied. Upgrade location settings");
            break;
          case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
            Timber.e("Location settings are inadequate, and cannot be fixed here. Fix in Settings.");
            break;
          default:
            break;
        }
      });
  }

  private void notifyListenersOnConnected() {
    for (LocationEngineListener listener : locationListeners) {
      listener.onConnected();
    }
  }

  private void stopLocationUpdates() {
    fusedLocationClient.removeLocationUpdates(forwardingCallback)
      .addOnCompleteListener(task -> receivingUpdates = false);
  }
}

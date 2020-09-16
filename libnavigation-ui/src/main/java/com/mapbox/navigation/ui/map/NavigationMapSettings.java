package com.mapbox.navigation.ui.map;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.navigation.ui.camera.NavigationCamera;

import static com.mapbox.navigation.ui.map.MapFpsDelegate.DEFAULT_MAX_FPS_THRESHOLD;

class NavigationMapSettings implements Parcelable {

  private int cameraTrackingMode;
  @Nullable
  private int[] currentPadding;
  private boolean shouldUseDefaultPadding;
  private int maxFps = DEFAULT_MAX_FPS_THRESHOLD;
  private boolean maxFpsEnabled = true;
  private boolean mapWayNameEnabled;
  private boolean locationFpsEnabled = true;
  private boolean vanishingRouteLineEnabled;

  NavigationMapSettings() {
  }

  void updateCameraTrackingMode(@NavigationCamera.TrackingMode int cameraTrackingMode) {
    this.cameraTrackingMode = cameraTrackingMode;
  }

  @NavigationCamera.TrackingMode
  int retrieveCameraTrackingMode() {
    return cameraTrackingMode;
  }

  void updateCurrentPadding(int[] currentPadding) {
    this.currentPadding = currentPadding;
  }

  @Nullable
  int[] retrieveCurrentPadding() {
    return currentPadding;
  }

  void updateShouldUseDefaultPadding(boolean shouldUseDefaultPadding) {
    this.shouldUseDefaultPadding = shouldUseDefaultPadding;
  }

  boolean shouldUseDefaultPadding() {
    return shouldUseDefaultPadding;
  }

  void updateMaxFps(int maxFps) {
    this.maxFps = maxFps;
  }

  int retrieveMaxFps() {
    return maxFps;
  }

  void updateMaxFpsEnabled(boolean maxFpsEnabled) {
    this.maxFpsEnabled = maxFpsEnabled;
  }

  boolean isMaxFpsEnabled() {
    return maxFpsEnabled;
  }

  void updateWayNameEnabled(boolean mapWayNameEnabled) {
    this.mapWayNameEnabled = mapWayNameEnabled;
  }

  boolean isMapWayNameEnabled() {
    return mapWayNameEnabled;
  }

  void updateLocationFpsEnabled(boolean locationFpsEnabled) {
    this.locationFpsEnabled = locationFpsEnabled;
  }

  void updateVanishingRouteLineEnabled(boolean enabled) {
    this.vanishingRouteLineEnabled = enabled;
  }

  boolean retrieveVanishingRouteLineEnabled() {
    return this.vanishingRouteLineEnabled;
  }

  boolean isLocationFpsEnabled() {
    return locationFpsEnabled;
  }

  private NavigationMapSettings(@NonNull Parcel in) {
    cameraTrackingMode = in.readInt();
    currentPadding = in.createIntArray();
    shouldUseDefaultPadding = in.readByte() != 0;
    maxFps = in.readInt();
    maxFpsEnabled = in.readByte() != 0;
    mapWayNameEnabled = in.readByte() != 0;
    locationFpsEnabled = in.readByte() != 0;
    vanishingRouteLineEnabled = in.readByte() != 0;
  }

  @Override
  public void writeToParcel(@NonNull Parcel dest, int flags) {
    dest.writeInt(cameraTrackingMode);
    dest.writeIntArray(currentPadding);
    dest.writeByte((byte) (shouldUseDefaultPadding ? 1 : 0));
    dest.writeInt(maxFps);
    dest.writeByte((byte) (maxFpsEnabled ? 1 : 0));
    dest.writeByte((byte) (mapWayNameEnabled ? 1 : 0));
    dest.writeByte((byte) (locationFpsEnabled ? 1 : 0));
    dest.writeByte((byte) (vanishingRouteLineEnabled ? 1 : 0));
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<NavigationMapSettings> CREATOR = new Creator<NavigationMapSettings>() {
    @NonNull
    @Override
    public NavigationMapSettings createFromParcel(@NonNull Parcel in) {
      return new NavigationMapSettings(in);
    }

    @NonNull
    @Override
    public NavigationMapSettings[] newArray(int size) {
      return new NavigationMapSettings[size];
    }
  };
}

package com.mapbox.services.android.navigation.ui.v5.map;

import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.services.android.navigation.ui.v5.ThrottleConfigFactory;
import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;

class NavigationMapSettings implements Parcelable {

  private int cameraTrackingMode;
  private int[] currentPadding;
  private boolean shouldUseDefaultPadding;
  private boolean mapWayNameEnabled;

  private boolean locationThrottlingEnabled = true;
  private ThrottleConfig locationThrottleConfig = ThrottleConfigFactory.defaultLocationProfile();

  private boolean mapThrottlingEnabled = true;
  private ThrottleConfig mapThrottleConfig = ThrottleConfigFactory.defaultMapProfile();

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

  int[] retrieveCurrentPadding() {
    return currentPadding;
  }

  void updateShouldUseDefaultPadding(boolean shouldUseDefaultPadding) {
    this.shouldUseDefaultPadding = shouldUseDefaultPadding;
  }

  boolean shouldUseDefaultPadding() {
    return shouldUseDefaultPadding;
  }

  void updateMapThrottlingEnabled(boolean mapThrottlingEnabled) {
    this.mapThrottlingEnabled = mapThrottlingEnabled;
  }

  boolean isMapThrottlingEnabled() {
    return mapThrottlingEnabled;
  }

  void updateMapThrottleConfig(ThrottleConfig throttleConfig) {
    this.mapThrottleConfig = throttleConfig;
  }

  ThrottleConfig retrieveMapThrottleConfig() {
    return mapThrottleConfig;
  }

  void updateLocationThrottlingEnabled(boolean locationThrottlingEnabled) {
    this.locationThrottlingEnabled = locationThrottlingEnabled;
  }

  boolean isLocationThrottlingEnabled() {
    return locationThrottlingEnabled;
  }

  void updateLocationThrottleConfig(ThrottleConfig throttleConfig) {
    this.locationThrottleConfig = throttleConfig;
  }

  ThrottleConfig retrieveLocationThrottleConfig() {
    return locationThrottleConfig;
  }

  void updateWayNameEnabled(boolean mapWayNameEnabled) {
    this.mapWayNameEnabled = mapWayNameEnabled;
  }

  boolean isMapWayNameEnabled() {
    return mapWayNameEnabled;
  }

  private NavigationMapSettings(Parcel in) {
    cameraTrackingMode = in.readInt();
    currentPadding = in.createIntArray();
    shouldUseDefaultPadding = in.readByte() != 0;
    mapWayNameEnabled = in.readByte() != 0;
    locationThrottlingEnabled = in.readByte() != 0;
    locationThrottleConfig = in.readParcelable(ThrottleConfig.class.getClassLoader());
    mapThrottlingEnabled = in.readByte() != 0;
    mapThrottleConfig = in.readParcelable(ThrottleConfig.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(cameraTrackingMode);
    dest.writeIntArray(currentPadding);
    dest.writeByte((byte) (shouldUseDefaultPadding ? 1 : 0));
    dest.writeByte((byte) (mapWayNameEnabled ? 1 : 0));
    dest.writeByte((byte) (locationThrottlingEnabled ? 1 : 0));
    dest.writeParcelable(locationThrottleConfig, flags);
    dest.writeByte((byte) (mapThrottlingEnabled ? 1 : 0));
    dest.writeParcelable(mapThrottleConfig, flags);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<NavigationMapSettings> CREATOR = new Creator<NavigationMapSettings>() {
    @Override
    public NavigationMapSettings createFromParcel(Parcel in) {
      return new NavigationMapSettings(in);
    }

    @Override
    public NavigationMapSettings[] newArray(int size) {
      return new NavigationMapSettings[size];
    }
  };
}

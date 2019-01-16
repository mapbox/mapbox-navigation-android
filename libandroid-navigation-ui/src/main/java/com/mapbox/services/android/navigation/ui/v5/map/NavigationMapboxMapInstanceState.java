package com.mapbox.services.android.navigation.ui.v5.map;

import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.services.android.navigation.ui.v5.camera.NavigationCamera;

public class NavigationMapboxMapInstanceState implements Parcelable {

  private final MapPaddingInstanceState mapPaddingInstanceState;
  @NavigationCamera.TrackingMode
  private final int cameraTrackingMode;
  private final MapFpsInstanceState mapFpsInstanceState;

  NavigationMapboxMapInstanceState(int[] currentPadding,
                                   boolean shouldUseDefaultPadding,
                                   @NavigationCamera.TrackingMode int cameraTrackingMode,
                                   int currentMaxFps,
                                   boolean maxFpsEnabled) {

    this.mapPaddingInstanceState = new MapPaddingInstanceState(currentPadding, shouldUseDefaultPadding);
    this.cameraTrackingMode = cameraTrackingMode;
    this.mapFpsInstanceState = new MapFpsInstanceState(currentMaxFps, maxFpsEnabled);
  }

  MapPaddingInstanceState retrieveMapPadding() {
    return mapPaddingInstanceState;
  }

  @NavigationCamera.TrackingMode
  int getCameraTrackingMode() {
    return cameraTrackingMode;
  }

  MapFpsInstanceState retrieveMapFps() {
    return mapFpsInstanceState;
  }

  private NavigationMapboxMapInstanceState(Parcel in) {
    mapPaddingInstanceState = in.readParcelable(MapPaddingInstanceState.class.getClassLoader());
    cameraTrackingMode = in.readInt();
    mapFpsInstanceState = in.readParcelable(MapFpsInstanceState.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(mapPaddingInstanceState, flags);
    dest.writeInt(cameraTrackingMode);
    dest.writeParcelable(mapFpsInstanceState, flags);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<NavigationMapboxMapInstanceState> CREATOR =
    new Creator<NavigationMapboxMapInstanceState>() {
    @Override
    public NavigationMapboxMapInstanceState createFromParcel(Parcel in) {
      return new NavigationMapboxMapInstanceState(in);
    }

    @Override
    public NavigationMapboxMapInstanceState[] newArray(int size) {
      return new NavigationMapboxMapInstanceState[size];
    }
  };
}

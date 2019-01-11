package com.mapbox.services.android.navigation.ui.v5.map;

import android.os.Parcel;
import android.os.Parcelable;

class MapFpsInstanceState implements Parcelable {

  private final int maxFps;
  private final boolean maxFpsEnabled;

  MapFpsInstanceState(int maxFps, boolean maxFpsEnabled) {
    this.maxFps = maxFps;
    this.maxFpsEnabled = maxFpsEnabled;
  }

  int retrieveMaxFps() {
    return maxFps;
  }

  boolean isMaxFpsEnabled() {
    return maxFpsEnabled;
  }

  private MapFpsInstanceState(Parcel in) {
    maxFps = in.readInt();
    maxFpsEnabled = in.readByte() != 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(maxFps);
    dest.writeByte((byte) (maxFpsEnabled ? 1 : 0));
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<MapFpsInstanceState> CREATOR = new Creator<MapFpsInstanceState>() {
    @Override
    public MapFpsInstanceState createFromParcel(Parcel in) {
      return new MapFpsInstanceState(in);
    }

    @Override
    public MapFpsInstanceState[] newArray(int size) {
      return new MapFpsInstanceState[size];
    }
  };
}

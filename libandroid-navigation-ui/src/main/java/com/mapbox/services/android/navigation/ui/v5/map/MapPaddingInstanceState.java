package com.mapbox.services.android.navigation.ui.v5.map;

import android.os.Parcel;
import android.os.Parcelable;

class MapPaddingInstanceState implements Parcelable {

  private final int[] currentPadding;
  private final boolean shouldUseDefault;

  MapPaddingInstanceState(int[] currentPadding, boolean shouldUseDefault) {
    this.currentPadding = currentPadding;
    this.shouldUseDefault = shouldUseDefault;
  }

  int[] retrieveCurrentPadding() {
    return currentPadding;
  }

  boolean shouldUseDefault() {
    return shouldUseDefault;
  }

  private MapPaddingInstanceState(Parcel in) {
    currentPadding = in.createIntArray();
    shouldUseDefault = in.readByte() != 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeIntArray(currentPadding);
    dest.writeByte((byte) (shouldUseDefault ? 1 : 0));
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<MapPaddingInstanceState> CREATOR = new Creator<MapPaddingInstanceState>() {
    @Override
    public MapPaddingInstanceState createFromParcel(Parcel in) {
      return new MapPaddingInstanceState(in);
    }

    @Override
    public MapPaddingInstanceState[] newArray(int size) {
      return new MapPaddingInstanceState[size];
    }
  };
}

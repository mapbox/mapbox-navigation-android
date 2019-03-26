package com.mapbox.services.android.navigation.ui.v5.map;


import android.os.Parcel;
import android.os.Parcelable;

public class ThrottleConfig implements Parcelable {

  private int[] levels = null;
  private ThrottleDomain throttleDomain;


  public enum ThrottleDomain {
    MAP, LOCATION
  }

  public ThrottleConfig(ThrottleDomain throttleDomain, int[] levels) {
    this.throttleDomain = throttleDomain;
    this.levels = levels;
  }

  int[] getLevels() {
    return levels;
  }

  private ThrottleConfig(Parcel in) {
    in.readIntArray(levels);
    throttleDomain = ThrottleDomain.values()[in.readInt()];
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeIntArray(levels);
    dest.writeInt(throttleDomain.ordinal());
  }

  public static final Creator<ThrottleConfig> CREATOR = new Creator<ThrottleConfig>() {
    @Override
    public ThrottleConfig createFromParcel(Parcel in) {
      return new ThrottleConfig(in);
    }

    @Override
    public ThrottleConfig[] newArray(int size) {
      return new ThrottleConfig[size];
    }
  };
}

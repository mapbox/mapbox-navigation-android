package com.mapbox.services.android.navigation.v5.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class LongCounter implements Parcelable {
  private final String name;
  private final long value;

  public LongCounter(String name, long value) {
    this.name = name;
    this.value = value;
  }

  private LongCounter(Parcel parcel) {
    this(parcel.readString(), parcel.readLong());
  }

  public String getName() {
    return name;
  }

  public long getValue() {
    return value;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int flags) {
    parcel.writeString(name);
    parcel.writeLong(value);
  }

  public static final Creator<LongCounter> CREATOR = new Creator<LongCounter>() {
    @Override
    public LongCounter createFromParcel(Parcel parcel) {
      return new LongCounter(parcel);
    }

    @Override
    public LongCounter[] newArray(int size) {
      return new LongCounter[size];
    }
  };
}

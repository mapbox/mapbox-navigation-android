package com.mapbox.services.android.navigation.v5.navigation;

import android.os.Parcel;

class LongCounter extends Counter<Long> {

  LongCounter(String name, Long value) {
    super(name, value);
  }

  private LongCounter(Parcel parcel) {
    super(parcel.readString(), parcel.readLong());
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

  public static Creator<Counter> CREATOR = new Creator<Counter>() {
    @Override
    public Counter createFromParcel(Parcel parcel) {
      return new LongCounter(parcel);
    }

    @Override
    public Counter[] newArray(int size) {
      return new Counter[size];
    }
  };
}

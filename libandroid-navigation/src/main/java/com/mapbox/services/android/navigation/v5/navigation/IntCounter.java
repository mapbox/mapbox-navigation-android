package com.mapbox.services.android.navigation.v5.navigation;

import android.os.Parcel;

public class IntCounter extends Counter<Integer> {

  IntCounter(String name, Integer value) {
    super(name, value);
  }

  private IntCounter(Parcel parcel) {
    super(parcel.readString(), parcel.readInt());
  }

  @Override
  public void writeToParcel(Parcel parcel, int flags) {
    parcel.writeString(name);
    parcel.writeInt(value);
  }

  public static Creator<Counter> CREATOR = new Creator<Counter>() {
    @Override
    public Counter createFromParcel(Parcel parcel) {
      return new IntCounter(parcel);
    }

    @Override
    public Counter[] newArray(int size) {
      return new Counter[size];
    }
  };
}

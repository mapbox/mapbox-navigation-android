package com.mapbox.services.android.navigation.v5.navigation;

import android.os.Parcel;

public class DoubleCounter extends Counter<Double> {

  DoubleCounter(String name, Double value) {
    super(name, value);
  }

  private DoubleCounter(Parcel parcel) {
    super(parcel.readString(), parcel.readDouble());
  }

  @Override
  public void writeToParcel(Parcel parcel, int flags) {
    parcel.writeString(name);
    parcel.writeDouble(value);
  }

  public static Creator<Counter> CREATOR = new Creator<Counter>() {
    @Override
    public Counter createFromParcel(Parcel parcel) {
      return new DoubleCounter(parcel);
    }

    @Override
    public Counter[] newArray(int size) {
      return new Counter[size];
    }
  };
}

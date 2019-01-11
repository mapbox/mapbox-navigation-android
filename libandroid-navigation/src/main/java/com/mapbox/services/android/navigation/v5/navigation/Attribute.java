package com.mapbox.services.android.navigation.v5.navigation;

import android.os.Parcel;
import android.os.Parcelable;

public class Attribute implements Parcelable {
  private final String name;
  private final String value;

  public Attribute(String name, String value) {
    this.name = name;
    this.value = value;
  }

  private Attribute(Parcel parcel) {
    this(parcel.readString(), parcel.readString());
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel parcel, int flags) {
    parcel.writeString(name);
    parcel.writeString(value);
  }

  public static final Creator<Attribute> CREATOR = new Creator<Attribute>() {
    @Override
    public Attribute createFromParcel(Parcel parcel) {
      return new Attribute(parcel);
    }

    @Override
    public Attribute[] newArray(int size) {
      return new Attribute[size];
    }
  };
}

package com.mapbox.services.android.navigation.v5.navigation;

import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public abstract class Counter<N extends Number> implements Parcelable {
  @SerializedName("name")
  protected final String name;
  @SerializedName("value")
  protected final N value;

  Counter(String name, N value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public String getName() {
    return name;
  }

  public N getValue() {
    return value;
  }
}

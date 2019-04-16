package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.os.Parcel;
import android.os.Parcelable;

public class NavigationNewData implements Parcelable {
  private Integer newDistanceRemaining;
  private Integer newDurationRemaining;
  private String newGeometry;

  public NavigationNewData(int newDistanceRemaining, int newDurationRemaining, String newGeometry) {
    this.newDistanceRemaining = newDistanceRemaining;
    this.newDurationRemaining = newDurationRemaining;
    this.newGeometry = newGeometry;
  }

  Integer getNewDistanceRemaining() {
    return newDistanceRemaining;
  }

  Integer getNewDurationRemaining() {
    return newDurationRemaining;
  }

  String getNewGeometry() {
    return newGeometry;
  }

  private NavigationNewData(Parcel in) {
    if (in.readByte() == 0) {
      newDistanceRemaining = null;
    } else {
      newDistanceRemaining = in.readInt();
    }
    if (in.readByte() == 0) {
      newDurationRemaining = null;
    } else {
      newDurationRemaining = in.readInt();
    }
    newGeometry = in.readString();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    if (newDistanceRemaining == null) {
      dest.writeByte((byte) 0);
    } else {
      dest.writeByte((byte) 1);
      dest.writeInt(newDistanceRemaining);
    }
    if (newDurationRemaining == null) {
      dest.writeByte((byte) 0);
    } else {
      dest.writeByte((byte) 1);
      dest.writeInt(newDurationRemaining);
    }
    dest.writeString(newGeometry);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @SuppressWarnings("unused")
  public static final Creator<NavigationNewData> CREATOR = new Creator<NavigationNewData>() {
    @Override
    public NavigationNewData createFromParcel(Parcel in) {
      return new NavigationNewData(in);
    }

    @Override
    public NavigationNewData[] newArray(int size) {
      return new NavigationNewData[size];
    }
  };
}

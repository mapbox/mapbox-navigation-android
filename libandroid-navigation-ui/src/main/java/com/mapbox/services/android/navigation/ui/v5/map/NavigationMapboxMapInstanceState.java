package com.mapbox.services.android.navigation.ui.v5.map;

import android.os.Parcel;
import android.os.Parcelable;

public class NavigationMapboxMapInstanceState implements Parcelable {

  private final boolean isWaynameVisible;
  private final String waynameText;
  private final boolean isCameraTracking;

  NavigationMapboxMapInstanceState(boolean isWaynameVisible, String waynameText, boolean isCameraTracking) {
    this.isWaynameVisible = isWaynameVisible;
    this.waynameText = waynameText;
    this.isCameraTracking = isCameraTracking;
  }

  public boolean isWaynameVisible() {
    return isWaynameVisible;
  }

  public String retrieveWayname() {
    return waynameText;
  }

  public boolean isCameraTracking() {
    return isCameraTracking;
  }

  private NavigationMapboxMapInstanceState(Parcel in) {
    isWaynameVisible = in.readByte() != 0;
    waynameText = in.readString();
    isCameraTracking = in.readByte() != 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeByte((byte) (isWaynameVisible ? 1 : 0));
    dest.writeString(waynameText);
    dest.writeByte((byte) (isCameraTracking ? 1 : 0));
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<NavigationMapboxMapInstanceState> CREATOR =
    new Creator<NavigationMapboxMapInstanceState>() {
    @Override
    public NavigationMapboxMapInstanceState createFromParcel(Parcel in) {
      return new NavigationMapboxMapInstanceState(in);
    }

    @Override
    public NavigationMapboxMapInstanceState[] newArray(int size) {
      return new NavigationMapboxMapInstanceState[size];
    }
  };
}

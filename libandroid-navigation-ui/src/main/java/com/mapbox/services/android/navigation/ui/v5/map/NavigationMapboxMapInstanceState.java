package com.mapbox.services.android.navigation.ui.v5.map;

import android.os.Parcel;
import android.os.Parcelable;

public class NavigationMapboxMapInstanceState implements Parcelable {

  private final NavigationMapSettings settings;

  NavigationMapboxMapInstanceState(NavigationMapSettings settings) {
    this.settings = settings;
  }

  NavigationMapSettings retrieveSettings() {
    return settings;
  }

  private NavigationMapboxMapInstanceState(Parcel in) {
    settings = in.readParcelable(NavigationMapSettings.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(settings, flags);
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

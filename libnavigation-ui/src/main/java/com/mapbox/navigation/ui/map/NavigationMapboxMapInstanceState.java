package com.mapbox.navigation.ui.map;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class NavigationMapboxMapInstanceState implements Parcelable {

  @Nullable
  private final NavigationMapSettings settings;

  NavigationMapboxMapInstanceState(NavigationMapSettings settings) {
    this.settings = settings;
  }

  @Nullable
  NavigationMapSettings retrieveSettings() {
    return settings;
  }

  private NavigationMapboxMapInstanceState(@NonNull Parcel in) {
    settings = in.readParcelable(NavigationMapSettings.class.getClassLoader());
  }

  @Override
  public void writeToParcel(@NonNull Parcel dest, int flags) {
    dest.writeParcelable(settings, flags);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<NavigationMapboxMapInstanceState> CREATOR =
    new Creator<NavigationMapboxMapInstanceState>() {
      @NonNull
      @Override
      public NavigationMapboxMapInstanceState createFromParcel(@NonNull Parcel in) {
        return new NavigationMapboxMapInstanceState(in);
      }

      @NonNull
      @Override
      public NavigationMapboxMapInstanceState[] newArray(int size) {
        return new NavigationMapboxMapInstanceState[size];
      }
    };
}

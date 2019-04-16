package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.JsonAdapter;
import com.mapbox.android.telemetry.Event;

class NavigationCancelEvent extends Event implements Parcelable {
  private static final String NAVIGATION_CANCEL = "navigation.cancel";
  private final String event;
  private NavigationCancelData cancelData;
  private NavigationMetadata metadata;

  NavigationCancelEvent(NavigationState navigationState) {
    this.event = NAVIGATION_CANCEL;
    this.cancelData = navigationState.getNavigationCancelData();
    this.metadata = navigationState.getNavigationMetadata();
  }

  Type obtainType() {
    return Type.NAV_CANCEL;
  }

  String getEvent() {
    return event;
  }

  NavigationCancelData getCancelData() {
    return cancelData;
  }

  NavigationMetadata getMetadata() {
    return metadata;
  }

  private NavigationCancelEvent(Parcel in) {
    event = in.readString();
    cancelData = in.readParcelable(NavigationCancelData.class.getClassLoader());
    metadata = in.readParcelable(NavigationMetadata.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(event);
    dest.writeParcelable(cancelData, flags);
    dest.writeParcelable(metadata, flags);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @SuppressWarnings("unused")
  public static final Creator<NavigationCancelEvent> CREATOR = new Creator<NavigationCancelEvent>() {
    @Override
    public NavigationCancelEvent createFromParcel(Parcel in) {
      return new NavigationCancelEvent(in);
    }

    @Override
    public NavigationCancelEvent[] newArray(int size) {
      return new NavigationCancelEvent[size];
    }
  };
}

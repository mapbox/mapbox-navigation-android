package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.JsonAdapter;
import com.mapbox.android.telemetry.Event;

class NavigationDepartEvent extends Event implements Parcelable {
  private static final String NAVIGATION_DEPART = "navigation.depart";
  private final String event;
  @JsonAdapter(NavigationMetadataSerializer.class)
  private NavigationMetadata metadata;

  NavigationDepartEvent(NavigationState navigationState) {
    this.event = NAVIGATION_DEPART;
    this.metadata = navigationState.getNavigationMetadata();
  }

  Type obtainType() {
    return Type.NAV_DEPART;
  }

  String getEvent() {
    return event;
  }

  NavigationMetadata getMetadata() {
    return metadata;
  }

  private NavigationDepartEvent(Parcel in) {
    event = in.readString();
    metadata = in.readParcelable(NavigationMetadata.class.getClassLoader());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(event);
    dest.writeParcelable(metadata, flags);
  }

  @SuppressWarnings("unused")
  public static final Creator<NavigationDepartEvent> CREATOR = new Creator<NavigationDepartEvent>() {
    @Override
    public NavigationDepartEvent createFromParcel(Parcel in) {
      return new NavigationDepartEvent(in);
    }

    @Override
    public NavigationDepartEvent[] newArray(int size) {
      return new NavigationDepartEvent[size];
    }
  };
}
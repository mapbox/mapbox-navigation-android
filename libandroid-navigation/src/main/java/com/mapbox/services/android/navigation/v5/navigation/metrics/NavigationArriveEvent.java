package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.JsonAdapter;
import com.mapbox.android.telemetry.Event;

class NavigationArriveEvent extends Event implements Parcelable {
  private static final String NAVIGATION_ARRIVE = "navigation.arrive";
  private final String event;
  @JsonAdapter(NavigationMetadataSerializer.class)
  private NavigationMetadata metadata;

  NavigationArriveEvent(NavigationState navigationState) {
    this.event = NAVIGATION_ARRIVE;
    this.metadata = navigationState.getNavigationMetadata();
  }

  Type obtainType() {
    return Type.NAV_ARRIVE;
  }

  String getEvent() {
    return event;
  }

  NavigationMetadata getMetadata() {
    return metadata;
  }

  private NavigationArriveEvent(Parcel in) {
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
  public static final Creator<NavigationArriveEvent> CREATOR = new Creator<NavigationArriveEvent>() {
    @Override
    public NavigationArriveEvent createFromParcel(Parcel in) {
      return new NavigationArriveEvent(in);
    }

    @Override
    public NavigationArriveEvent[] newArray(int size) {
      return new NavigationArriveEvent[size];
    }
  };
}

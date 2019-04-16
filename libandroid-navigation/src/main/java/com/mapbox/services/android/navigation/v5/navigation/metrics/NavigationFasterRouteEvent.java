package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.JsonAdapter;
import com.mapbox.android.telemetry.Event;

class NavigationFasterRouteEvent extends Event implements Parcelable {
  private static final String NAVIGATION_REROUTE_DATA_STATE_ILLEGAL_NULL = "NavigationRerouteData cannot be null.";
  private static final String NAVIGATION_FASTER_ROUTE = "navigation.fasterRoute";
  private final String event;
  @JsonAdapter(NavigationMetadataSerializer.class)
  private NavigationMetadata metadata = null;
  private NavigationNewData navigationNewData = null;
  private NavigationStepMetadata step = null;

  NavigationFasterRouteEvent(NavigationState navigationState) {
    this.event = NAVIGATION_FASTER_ROUTE;
    NavigationRerouteData navigationRerouteData = navigationState.getNavigationRerouteData();
    check(navigationRerouteData);
    this.navigationNewData = navigationRerouteData.getNavigationNewData();
    this.step = navigationState.getNavigationStepMetadata();
    this.metadata = navigationState.getNavigationMetadata();
  }

  Type obtainType() {
    return Type.NAV_FASTER_ROUTE;
  }

  String getEvent() {
    return event;
  }

  NavigationNewData getNavigationNewData() {
    return navigationNewData;
  }

  NavigationStepMetadata getStep() {
    return step;
  }

  NavigationMetadata getMetadata() {
    return metadata;
  }

  private NavigationFasterRouteEvent(Parcel in) {
    event = in.readString();
    navigationNewData = in.readParcelable(NavigationNewData.class.getClassLoader());
    step = in.readParcelable(NavigationStepMetadata.class.getClassLoader());
    metadata = in.readParcelable(NavigationMetadata.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(event);
    dest.writeParcelable(navigationNewData, flags);
    dest.writeParcelable(step, flags);
    dest.writeParcelable(metadata, flags);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @SuppressWarnings("unused")
  public static final Creator<NavigationFasterRouteEvent> CREATOR = new Creator<NavigationFasterRouteEvent>() {
    @Override
    public NavigationFasterRouteEvent createFromParcel(Parcel in) {
      return new NavigationFasterRouteEvent(in);
    }

    @Override
    public NavigationFasterRouteEvent[] newArray(int size) {
      return new NavigationFasterRouteEvent[size];
    }
  };

  private void check(NavigationRerouteData navigationRerouteData) {
    if (navigationRerouteData == null) {
      throw new IllegalArgumentException(NAVIGATION_REROUTE_DATA_STATE_ILLEGAL_NULL);
    }
  }
}

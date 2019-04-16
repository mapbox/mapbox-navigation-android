package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.JsonAdapter;
import com.mapbox.android.telemetry.Event;

class NavigationRerouteEvent extends Event implements Parcelable {
  private static final String NAVIGATION_REROUTE = "navigation.reroute";
  private final String event;
  private NavigationMetadata navigationMetadata;
  private NavigationRerouteData navigationRerouteData;
  private NavigationLocationData navigationLocationData;
  private FeedbackData feedbackData;
  private NavigationStepMetadata step = null;

  NavigationRerouteEvent(NavigationState navigationState) {
    this.event = NAVIGATION_REROUTE;
    this.feedbackData = navigationState.getFeedbackData();
    this.navigationMetadata = navigationState.getNavigationMetadata();
    this.navigationRerouteData = navigationState.getNavigationRerouteData();
    this.navigationLocationData = navigationState.getNavigationLocationData();
    this.step = navigationState.getNavigationStepMetadata();
  }

  Type obtainType() {
    return Type.NAV_REROUTE;
  }

  String getEvent() {
    return event;
  }

  NavigationLocationData getNavigationLocationData() {
    return navigationLocationData;
  }

  NavigationRerouteData getNavigationRerouteData() {
    return navigationRerouteData;
  }

  NavigationStepMetadata getStep() {
    return step;
  }

  FeedbackData getFeedbackData() {
    return feedbackData;
  }

  NavigationMetadata getNavigationMetadata() {
    return navigationMetadata;
  }

  private NavigationRerouteEvent(Parcel in) {
    event = in.readString();
    navigationMetadata = in.readParcelable(NavigationMetadata.class.getClassLoader());
    navigationLocationData = in.readParcelable(NavigationLocationData.class.getClassLoader());
    feedbackData = in.readParcelable(FeedbackData.class.getClassLoader());
    step = in.readParcelable(NavigationStepMetadata.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(event);
    dest.writeParcelable(navigationMetadata, flags);
    dest.writeParcelable(navigationLocationData, flags);
    dest.writeParcelable(feedbackData, flags);
    dest.writeParcelable(step, flags);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @SuppressWarnings("unused")
  public static final Creator<NavigationRerouteEvent> CREATOR = new Creator<NavigationRerouteEvent>() {
    @Override
    public NavigationRerouteEvent createFromParcel(Parcel in) {
      return new NavigationRerouteEvent(in);
    }

    @Override
    public NavigationRerouteEvent[] newArray(int size) {
      return new NavigationRerouteEvent[size];
    }
  };
}

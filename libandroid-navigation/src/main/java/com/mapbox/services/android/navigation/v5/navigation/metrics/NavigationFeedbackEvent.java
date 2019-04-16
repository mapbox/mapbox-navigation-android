package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.JsonAdapter;
import com.mapbox.android.telemetry.Event;

class NavigationFeedbackEvent extends Event implements Parcelable {
  private static final String NAVIGATION_FEEDBACK = "navigation.feedback";
  private final String event;
  private NavigationMetadata metadata;
  private FeedbackEventData feedbackEventData;
  private NavigationLocationData navigationLocationData;
  private FeedbackData feedbackData;
  private NavigationStepMetadata step = null;

  NavigationFeedbackEvent(NavigationState navigationState) {
    this.event = NAVIGATION_FEEDBACK;
    this.metadata = navigationState.getNavigationMetadata();
    this.feedbackEventData = navigationState.getFeedbackEventData();
    this.navigationLocationData = navigationState.getNavigationLocationData();
    this.feedbackData = navigationState.getFeedbackData();
    this.step = navigationState.getNavigationStepMetadata();
  }

  Type obtainType() {
    return Type.NAV_FEEDBACK;
  }

  String getEvent() {
    return event;
  }

  NavigationMetadata getMetadata() {
    return metadata;
  }

  FeedbackEventData getFeedbackEventData() {
    return feedbackEventData;
  }

  NavigationLocationData getNavigationLocationData() {
    return navigationLocationData;
  }

  FeedbackData getFeedbackData() {
    return feedbackData;
  }

  NavigationStepMetadata getStep() {
    return step;
  }

  private NavigationFeedbackEvent(Parcel in) {
    event = in.readString();
    metadata = (NavigationMetadata) in.readValue(NavigationMetadata.class.getClassLoader());
    feedbackEventData = (FeedbackEventData) in.readValue(FeedbackEventData.class.getClassLoader());
    navigationLocationData = (NavigationLocationData) in.readValue(NavigationLocationData.class.getClassLoader());
    feedbackData = (FeedbackData) in.readValue(FeedbackData.class.getClassLoader());
    step = (NavigationStepMetadata) in.readValue(NavigationStepMetadata.class.getClassLoader());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(event);
    dest.writeValue(metadata);
    dest.writeValue(feedbackEventData);
    dest.writeValue(navigationLocationData);
    dest.writeValue(feedbackData);
    dest.writeValue(step);
  }

  @SuppressWarnings("unused")
  public static final Creator<NavigationFeedbackEvent> CREATOR =
    new Creator<NavigationFeedbackEvent>() {
      @Override
      public NavigationFeedbackEvent createFromParcel(Parcel in) {
        return new NavigationFeedbackEvent(in);
      }

      @Override
      public NavigationFeedbackEvent[] newArray(int size) {
        return new NavigationFeedbackEvent[size];
      }
    };
}

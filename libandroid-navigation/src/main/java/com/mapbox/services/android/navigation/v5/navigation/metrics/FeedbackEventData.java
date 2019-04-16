package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.android.telemetry.TelemetryUtils;

public class FeedbackEventData implements Parcelable {
  private String userId;
  private String feedbackType;
  private String source;
  private String description = null;

  public FeedbackEventData(String feedbackType, String source) {
    this.userId = TelemetryUtils.retrieveVendorId();
    this.feedbackType = feedbackType;
    this.source = source;
  }

  // For testing only
  FeedbackEventData(String userId, String feedbackType, String source) {
    this.userId = userId;
    this.feedbackType = feedbackType;
    this.source = source;
  }

  String getUserId() {
    return userId;
  }

  String getFeedbackType() {
    return feedbackType;
  }

  String getSource() {
    return source;
  }

  String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  private FeedbackEventData(Parcel in) {
    userId = in.readString();
    feedbackType = in.readString();
    source = in.readString();
    description = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(userId);
    dest.writeString(feedbackType);
    dest.writeString(source);
    dest.writeString(description);
  }

  @SuppressWarnings("unused")
  public static final Creator<FeedbackEventData> CREATOR = new Creator<FeedbackEventData>() {
    @Override
    public FeedbackEventData createFromParcel(Parcel in) {
      return new FeedbackEventData(in);
    }

    @Override
    public FeedbackEventData[] newArray(int size) {
      return new FeedbackEventData[size];
    }
  };
}
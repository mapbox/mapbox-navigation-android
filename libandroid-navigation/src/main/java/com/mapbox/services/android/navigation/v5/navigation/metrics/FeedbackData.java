package com.mapbox.services.android.navigation.v5.navigation.metrics;


import android.os.Parcel;
import android.os.Parcelable;

import com.mapbox.android.telemetry.TelemetryUtils;

public class FeedbackData implements Parcelable {
  private String feedbackId;
  private String screenshot = null;

  public FeedbackData() {
    this.feedbackId = TelemetryUtils.obtainUniversalUniqueIdentifier();
  }

  String getFeedbackId() {
    return feedbackId;
  }

  String getScreenshot() {
    return screenshot;
  }

  public void setScreenshot(String screenshot) {
    this.screenshot = screenshot;
  }

  private FeedbackData(Parcel in) {
    feedbackId = in.readString();
    screenshot = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(feedbackId);
    dest.writeString(screenshot);
  }

  @SuppressWarnings("unused")
  public static final Creator<FeedbackData> CREATOR = new Creator<FeedbackData>() {
    @Override
    public FeedbackData createFromParcel(Parcel in) {
      return new FeedbackData(in);
    }

    @Override
    public FeedbackData[] newArray(int size) {
      return new FeedbackData[size];
    }
  };
}

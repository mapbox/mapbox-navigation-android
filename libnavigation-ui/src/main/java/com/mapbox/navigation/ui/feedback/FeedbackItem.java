package com.mapbox.navigation.ui.feedback;

import com.mapbox.navigation.core.telemetry.events.TelemetryUserFeedback;

public class FeedbackItem {

  @TelemetryUserFeedback.FeedbackType
  private String feedbackType;
  private String feedbackText;
  private String description;
  private int feedbackImage;

  FeedbackItem(String feedbackText,
      int feedbackImage,
      @TelemetryUserFeedback.FeedbackType
          String feedbackType,
      String description) {
    this.feedbackText = feedbackText;
    this.feedbackImage = feedbackImage;
    this.feedbackType = feedbackType;
    this.description = description;
  }

  String getFeedbackText() {
    return feedbackText;
  }

  int getFeedbackImageId() {
    return feedbackImage;
  }

  @TelemetryUserFeedback.FeedbackType
  public String getFeedbackType() {
    return feedbackType;
  }

  public String getDescription() {
    return description;
  }
}

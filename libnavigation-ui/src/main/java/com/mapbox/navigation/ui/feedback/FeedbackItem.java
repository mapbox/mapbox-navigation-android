package com.mapbox.navigation.ui.feedback;

import com.mapbox.navigation.core.telemetry.events.FeedbackEvent;

public class FeedbackItem {

  @FeedbackEvent.Type
  private String feedbackType;
  private String feedbackText;
  private String description;
  private int feedbackImage;

  FeedbackItem(String feedbackText,
      int feedbackImage,
      @FeedbackEvent.Type
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

  @FeedbackEvent.Type
  public String getFeedbackType() {
    return feedbackType;
  }

  public String getDescription() {
    return description;
  }
}

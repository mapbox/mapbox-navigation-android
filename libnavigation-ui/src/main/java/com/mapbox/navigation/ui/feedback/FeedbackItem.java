package com.mapbox.navigation.ui.feedback;

public class FeedbackItem {

  //  @FeedbackEvent.FeedbackType // TODO Telemetry Impl
  private String feedbackType;
  private String feedbackText;
  private String description;
  private int feedbackImage;

  FeedbackItem(String feedbackText,
               int feedbackImage,
//               @FeedbackEvent.FeedbackType // TODO Telemetry Impl
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

//  @FeedbackEvent.FeedbackType // TODO Telemetry Impl
  public String getFeedbackType() {
    return feedbackType;
  }

  public String getDescription() {
    return description;
  }
}

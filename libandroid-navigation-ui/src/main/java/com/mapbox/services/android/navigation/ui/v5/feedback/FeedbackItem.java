package com.mapbox.services.android.navigation.ui.v5.feedback;

public class FeedbackItem {

  private String feedbackText;
  private int feedbackImage;

  private String feedbackId;
  private String feedbackType;
  private String description;

  FeedbackItem(String feedbackText,
               int feedbackImage,
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

  String getFeedbackType() {
    return feedbackType;
  }

  public String getDescription() {
    return description;
  }

  public String getFeedbackId() {
    return feedbackId;
  }

  public void setFeedbackId(String feedbackId) {
    this.feedbackId = feedbackId;
  }
}

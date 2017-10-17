package com.mapbox.services.android.navigation.ui.v5.feedback;

class FeedbackModel {

  private int feedbackBackground;
  private String feedbackText;
  private int feedbackImage;

  FeedbackModel(int feedbackBackground,
                       String feedbackText,
                       int feedbackImage) {
    this.feedbackBackground = feedbackBackground;
    this.feedbackText = feedbackText;
    this.feedbackImage = feedbackImage;
  }

  int getFeedbackBackgroundId() {
    return feedbackBackground;
  }

  String getFeedbackText() {
    return feedbackText;
  }

  int getFeedbackImageId() {
    return feedbackImage;
  }
}

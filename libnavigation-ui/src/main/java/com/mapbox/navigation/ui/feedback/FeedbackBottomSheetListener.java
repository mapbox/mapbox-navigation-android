package com.mapbox.navigation.ui.feedback;

public interface FeedbackBottomSheetListener {

  void onFeedbackSelected(FeedbackItem feedbackItem);

  void onFeedbackDismissed();
}

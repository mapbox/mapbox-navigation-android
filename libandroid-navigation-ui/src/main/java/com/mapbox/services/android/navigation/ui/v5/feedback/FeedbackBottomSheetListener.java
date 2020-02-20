package com.mapbox.services.android.navigation.ui.v5.feedback;

public interface FeedbackBottomSheetListener {

  void onFeedbackSelected(FeedbackItem feedbackItem);

  void onFeedbackDismissed();
}

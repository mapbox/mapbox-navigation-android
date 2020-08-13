package com.mapbox.navigation.ui.feedback;

/**
 * Interface notified on interaction with {@link FeedbackBottomSheet}.
 */
public interface FeedbackBottomSheetListener {

  void onFeedbackSelected(FeedbackItem feedbackItem);

  void onFeedbackDismissed();
}

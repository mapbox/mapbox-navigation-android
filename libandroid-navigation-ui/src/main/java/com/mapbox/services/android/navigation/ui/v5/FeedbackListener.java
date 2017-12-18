package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;

public interface FeedbackListener {
  void onFeedbackOpened();

  void onFeedbackCancelled();

  void onFeedbackSent(FeedbackItem feedbackItem);
}

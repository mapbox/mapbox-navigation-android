package com.mapbox.navigation.ui.listeners;

import com.mapbox.navigation.ui.NavigationView;
import com.mapbox.navigation.ui.NavigationViewOptions;
import com.mapbox.navigation.ui.feedback.FeedbackItem;

/**
 * A listener that can be implemented and
 * added to {@link NavigationViewOptions} to
 * hook into feedback related events occurring in {@link NavigationView}.
 */
public interface FeedbackListener {

  /**
   * Will be triggered when the feedback bottomsheet
   * is opened by a user while navigating.
   */
  void onFeedbackOpened();

  /**
   * Will be triggered when the feedback bottomsheet
   * is opened by a user while navigating but then dismissed
   * without clicking on a specific {@link FeedbackItem} in the list.
   */
  void onFeedbackCancelled();

  /**
   * Will be triggered when the feedback bottomsheet
   * is opened by a user while navigating and then the user
   * clicks on a specific {@link FeedbackItem} in the list.
   *
   * @param feedbackItem that was clicked
   */
  void onFeedbackSent(FeedbackItem feedbackItem);
}

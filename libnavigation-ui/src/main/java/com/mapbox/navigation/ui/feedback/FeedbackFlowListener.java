package com.mapbox.navigation.ui.feedback;

/**
 * Interface notified on interaction with the possible feedback opportunities
 * that the Navigation UI SDK provides.
 */
public interface FeedbackFlowListener {

  void onDetailedFeedbackFlowFinished();

  void onArrivalExperienceFeedbackFinished(FeedbackItem arrivalFeedbackItem);
}

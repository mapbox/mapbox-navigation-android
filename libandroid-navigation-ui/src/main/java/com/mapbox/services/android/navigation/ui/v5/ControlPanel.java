package com.mapbox.services.android.navigation.ui.v5;

import android.support.annotation.Nullable;

import com.mapbox.services.android.navigation.ui.v5.instruction.NavigationAlertView;

/**
 * This class encapsulates the view control logic
 */
class ControlPanel {
  private SoundButton soundButton;
  private NavigationButton feedbackButton;
  private NavigationAlertView navigationAlertView;

  ControlPanel(@Nullable SoundButton soundButton, @Nullable NavigationButton feedbackButton,
               @Nullable NavigationAlertView navigationAlertView) {
    this.soundButton = soundButton;
    this.feedbackButton = feedbackButton;
    this.navigationAlertView = navigationAlertView;
  }

  void initializeClickListeners(NavigationPresenter navigationPresenter) {
    if (feedbackButton != null) {
      feedbackButton.addOnClickListener(new FeedbackBtnClickListener(navigationPresenter));
    }
    if (soundButton != null) {
      soundButton.addOnClickListener(new SoundBtnClickListener(navigationPresenter));
    }
  }

  void setMuted(boolean isMuted) {
    if (soundButton != null) {
      soundButton.setMuted(isMuted);
    }
  }

  boolean toggleMute() {
    if (soundButton != null) {
      return soundButton.toggleMute();
    }

    return false;
  }

  void showReportProblem() {
    if (navigationAlertView != null) {
      navigationAlertView.showReportProblem();
    }
  }

  void showButtons() {
    if (feedbackButton != null) {
      feedbackButton.show();
    }

    if (soundButton != null) {
      soundButton.show();
    }
  }

  void hideButtons() {
    if (feedbackButton != null) {
      feedbackButton.hide();
    }

    if (soundButton != null) {
      soundButton.hide();
    }
  }

  void showFeedbackSubmitted() {
    navigationAlertView.showFeedbackSubmitted();
  }

  void subscribe(NavigationViewModel navigationViewModel) {
    navigationAlertView.subscribe(navigationViewModel);
  }
}

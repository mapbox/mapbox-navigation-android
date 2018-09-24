package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.View;

import com.mapbox.services.android.navigation.ui.v5.NavigationViewModel;
import com.mapbox.services.android.navigation.ui.v5.alert.AlertView;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackBottomSheet;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackBottomSheetListener;
import com.mapbox.services.android.navigation.ui.v5.feedback.FeedbackItem;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.metrics.FeedbackEvent;

import timber.log.Timber;

public class NavigationAlertView extends AlertView implements FeedbackBottomSheetListener {
  private NavigationViewModel navigationViewModel;
  private static final long THREE_SECOND_DELAY_IN_MILLIS = 3000;

  public NavigationAlertView(Context context) {
    this(context, null);
  }

  public NavigationAlertView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public NavigationAlertView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  /**
   * Sets the NavigationViewModel in the view
   *
   * @param navigationViewModel to set
   */
  public void subscribe(NavigationViewModel navigationViewModel) {
    this.navigationViewModel = navigationViewModel;
  }

  /**
   * Shows this alert view for when feedback is submitted
   */
  public void showFeedbackSubmitted() {
    show(NavigationConstants.FEEDBACK_SUBMITTED, THREE_SECOND_DELAY_IN_MILLIS, false);
  }

  /**
   * Shows this alert view to let user report a problem for the given number of milliseconds
   */
  public void showReportProblem() {
    final Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        show(NavigationConstants.REPORT_PROBLEM,
          NavigationConstants.ALERT_VIEW_PROBLEM_DURATION, true);
      }
    }, THREE_SECOND_DELAY_IN_MILLIS);
  }

  /**
   * Shows {@link FeedbackBottomSheet} and adds a listener so
   * the proper feedback information is collected or the user dismisses the UI.
   */
  public void showFeedbackBottomSheet() {
    FragmentManager fragmentManager = obtainSupportFragmentManager();
    if (fragmentManager != null) {
      long duration = NavigationConstants.FEEDBACK_BOTTOM_SHEET_DURATION;
      FeedbackBottomSheet.newInstance(this, duration).show(fragmentManager, FeedbackBottomSheet.TAG);
    }
  }

  @Override
  public void onFeedbackSelected(FeedbackItem feedbackItem) {
    navigationViewModel.updateFeedback(feedbackItem);
    showFeedbackSubmitted();
  }

  @Override
  public void onFeedbackDismissed() {
    navigationViewModel.cancelFeedback();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (isShowingReportProblem()) {
          navigationViewModel.recordFeedback(FeedbackEvent.FEEDBACK_SOURCE_REROUTE);
          showFeedbackBottomSheet();
        }
        hide();
      }
    });
  }

  @Nullable
  private FragmentManager obtainSupportFragmentManager() {
    try {
      return ((FragmentActivity) getContext()).getSupportFragmentManager();
    } catch (ClassCastException exception) {
      Timber.e(exception);
      return null;
    }
  }

  private boolean isShowingReportProblem() {
    return getAlertText().equals(NavigationConstants.REPORT_PROBLEM);
  }
}

package com.mapbox.navigation.ui.instruction;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;

import com.mapbox.libnavigation.ui.R;
import com.mapbox.navigation.ui.NavigationView;
import com.mapbox.navigation.ui.NavigationViewModel;
import com.mapbox.navigation.ui.alert.AlertView;
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheet;
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheetListener;
import com.mapbox.navigation.ui.feedback.FeedbackItem;
import com.mapbox.navigation.ui.internal.navigation.metrics.FeedbackEvent;
import com.mapbox.navigation.utils.ConstantsEx;

import timber.log.Timber;

public class NavigationAlertView extends AlertView implements FeedbackBottomSheetListener {

  private static final long THREE_SECOND_DELAY_IN_MILLIS = 3000;
  private NavigationViewModel navigationViewModel;
  private boolean isEnabled = true;

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
    if (!isEnabled) {
      return;
    }
    show(getContext().getString(R.string.feedback_submitted), THREE_SECOND_DELAY_IN_MILLIS, false);
  }

  /**
   * Shows this alert view to let user report a problem for the given number of milliseconds
   */
  public void showReportProblem() {
    if (!isEnabled) {
      return;
    }
    final Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        show(getContext().getString(R.string.report_problem),
                ConstantsEx.ALERT_VIEW_PROBLEM_DURATION, true);
      }
    }, THREE_SECOND_DELAY_IN_MILLIS);
  }

  /**
   * Shows {@link FeedbackBottomSheet} and adds a listener so
   * the proper feedback information is collected or the user dismisses the UI.
   */
  public void showFeedbackBottomSheet() {
    if (!isEnabled) {
      return;
    }
    FragmentManager fragmentManager = obtainSupportFragmentManager();
    if (fragmentManager != null) {
      FeedbackBottomSheet.newInstance(this, ConstantsEx.FEEDBACK_BOTTOM_SHEET_DURATION)
              .show(fragmentManager, FeedbackBottomSheet.TAG);
    }
  }

  /**
   * This method enables or disables the alert view from being shown during off-route
   * events.
   * <p>
   * Note this will only happen automatically in the context of
   * the {@link NavigationView} or a {@link NavigationViewModel}
   * has been added to the instruction view with {@link InstructionView#subscribe(LifecycleOwner, NavigationViewModel)}.
   *
   * @param isEnabled true to show during off-route events, false to hide
   */
  public void updateEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  @Override
  public void onFeedbackSelected(FeedbackItem feedbackItem) {
    if (navigationViewModel == null) {
      return;
    }
    navigationViewModel.updateFeedback(feedbackItem);
    showFeedbackSubmitted();
  }

  @Override
  public void onFeedbackDismissed() {
    if (navigationViewModel == null) {
      return;
    }
    navigationViewModel.cancelFeedback();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (navigationViewModel != null && isShowingReportProblem()) {
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
    return getAlertText().equals(getContext().getString(R.string.report_problem));
  }
}

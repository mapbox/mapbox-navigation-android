package com.mapbox.navigation.ui.feedback;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mapbox.libnavigation.ui.R;
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A BottomSheetDialogFragment shows Feedback UI with different feedback categories.
 * <p>
 * This view takes a {@link FeedbackBottomSheetListener}.
 * The {@link FeedbackBottomSheetListener#onFeedbackSelected(FeedbackItem)} will be called
 * when a feedback category is selected.
 * The {@link FeedbackBottomSheetListener#onFeedbackDismissed()} will be called when this fragment dismiss.
 */
public class FeedbackBottomSheet extends BottomSheetDialogFragment implements Animator.AnimatorListener {

  public static final String TAG = FeedbackBottomSheet.class.getSimpleName();
  private static final String EMPTY_FEEDBACK_DESCRIPTION = "";
  private static final long CLOSE_BOTTOM_SHEET_AFTER = 150L;
  private static final long TIMER_INTERVAL = 1L;
  private static final int GRID_SPAN_GUIDANCE_LAYOUT = 2;
  private static final int GRID_SPAN_NAVIGATION_LAYOUT = 3;

  private FeedbackBottomSheetListener feedbackBottomSheetListener;
  private RecyclerView guidanceIssueItems;
  private FeedbackAdapter guidanceIssueAdapter;
  private RecyclerView navigationIssueItems;
  private FeedbackAdapter notificationIssueAdapter;
  private ImageButton cancelBtn;
  private ProgressBar feedbackProgressBar;
  private ObjectAnimator countdownAnimation;
  private long duration;
  private CountDownTimer timer = null;

  public static FeedbackBottomSheet newInstance(FeedbackBottomSheetListener feedbackBottomSheetListener,
                                                long duration) {
    FeedbackBottomSheet feedbackBottomSheet = new FeedbackBottomSheet();
    feedbackBottomSheet.setFeedbackBottomSheetListener(feedbackBottomSheetListener);
    feedbackBottomSheet.setDuration(duration);
    feedbackBottomSheet.setRetainInstance(true);
    return feedbackBottomSheet;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(BottomSheetDialogFragment.STYLE_NO_FRAME, R.style.Theme_Design_BottomSheetDialog);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.mapbox_feedback_bottom_sheet_layout, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    bind(view);
    initCancelButton();
    initFeedbackRecyclerView();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialog) {
        BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
        FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
          BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
          behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
          behavior.setSkipCollapsed(true);
        }
      }
    });
    return dialog;
  }

  @Override
  public void onDismiss(DialogInterface dialog) {
    super.onDismiss(dialog);
    feedbackBottomSheetListener.onFeedbackDismissed();
  }

  @Override
  public void onDestroyView() {
    removeListener();
    removeDialogDismissMessage();
    cancelCountdownAnimation();
    super.onDestroyView();
  }

  @Override
  public void onAnimationEnd(Animator animation) {
    FeedbackBottomSheet.this.dismiss();
  }

  //region Unused Listener Methods

  @Override
  public void onAnimationStart(Animator animation) {

  }

  @Override
  public void onAnimationCancel(Animator animation) {

  }

  @Override
  public void onAnimationRepeat(Animator animation) {

  }

  //endregion

  public void setFeedbackBottomSheetListener(FeedbackBottomSheetListener feedbackBottomSheetListener) {
    this.feedbackBottomSheetListener = feedbackBottomSheetListener;
  }

  /**
   * @param duration in milliseconds, the BottomSheet will show before being dismissed.
   */
  public void setDuration(long duration) {
    this.duration = duration;
  }

  private void bind(View bottomSheetView) {
    guidanceIssueItems = bottomSheetView.findViewById(R.id.guidanceIssueItems);
    navigationIssueItems = bottomSheetView.findViewById(R.id.navigationIssueItems);
    feedbackProgressBar = bottomSheetView.findViewById(R.id.feedbackProgress);
    cancelBtn = bottomSheetView.findViewById(R.id.cancelBtn);
  }

  private void initCancelButton() {
    cancelBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        dismiss();
      }
    });
  }

  private void initFeedbackRecyclerView() {
    final Context context = getContext();

    guidanceIssueAdapter = new FeedbackAdapter(buildGuidanceIssueList());
    guidanceIssueItems.setAdapter(guidanceIssueAdapter);
    guidanceIssueItems.setOverScrollMode(RecyclerView.OVER_SCROLL_IF_CONTENT_SCROLLS);
    guidanceIssueItems.addOnItemTouchListener(new FeedbackClickListener(context, guidanceIssueClickCallback));
    guidanceIssueItems.setLayoutManager(new GridLayoutManager(context, GRID_SPAN_GUIDANCE_LAYOUT));

    notificationIssueAdapter = new FeedbackAdapter(buildNavigationIssueList());
    navigationIssueItems.setAdapter(notificationIssueAdapter);
    navigationIssueItems.setOverScrollMode(RecyclerView.OVER_SCROLL_IF_CONTENT_SCROLLS);
    navigationIssueItems.addOnItemTouchListener(new FeedbackClickListener(context, navigationIssueClickCallback));
    navigationIssueItems.setLayoutManager(new GridLayoutManager(context, GRID_SPAN_NAVIGATION_LAYOUT));
  }

  private void initCountDownAnimation() {
    countdownAnimation = ObjectAnimator.ofInt(feedbackProgressBar,
      "progress", 0);
    countdownAnimation.setInterpolator(new LinearInterpolator());
    countdownAnimation.setDuration(duration);
    countdownAnimation.addListener(this);
    countdownAnimation.start();
  }

  private void removeListener() {
    feedbackBottomSheetListener = null;
  }

  private void removeDialogDismissMessage() {
    Dialog dialog = getDialog();
    if (dialog != null && getRetainInstance()) {
      dialog.setDismissMessage(null);
    }
  }

  private void cancelCountdownAnimation() {
    if (countdownAnimation != null) {
      countdownAnimation.removeAllListeners();
      countdownAnimation.cancel();
    }
  }

  private void startTimer() {
    if (timer != null) {
      timer.cancel();
    }
    timer = new CountDownTimer(CLOSE_BOTTOM_SHEET_AFTER, TIMER_INTERVAL) {
      @Override
      public void onTick(long millisUntilFinished) {
        // We don't need to observe changes in interval, hence left empty
      }

      @Override
      public void onFinish() {
        dismiss();
      }
    };
    timer.start();
  }

  @SuppressLint("WrongConstant")
  private List<FeedbackItem> buildGuidanceIssueList() {
    List<FeedbackItem> list = new ArrayList<>();

    list.add(new FeedbackItem(getResources().getString(R.string.feedback_type_incorrect_visual),
      R.drawable.ic_feedback_incorrect_visual,
      FeedbackEvent.INCORRECT_VISUAL_GUIDANCE,
      EMPTY_FEEDBACK_DESCRIPTION));
    list.add(new FeedbackItem(getResources().getString(R.string.feedback_type_confusing_audio),
      R.drawable.ic_feedback_confusing_audio,
      FeedbackEvent.INCORRECT_AUDIO_GUIDANCE,
      EMPTY_FEEDBACK_DESCRIPTION));

    return list;
  }

  private FeedbackClickListener.ClickCallback guidanceIssueClickCallback = new FeedbackClickListener.ClickCallback() {
    @Override
    public void onFeedbackItemClick(ImageView view, int feedbackPosition) {
      if (view != null) {
        view.setPressed(!view.isPressed());
      }
      FeedbackItem feedbackItem = guidanceIssueAdapter.getFeedbackItem(feedbackPosition);
      feedbackBottomSheetListener.onFeedbackSelected(feedbackItem);
      startTimer();
    }
  };

  @SuppressLint("WrongConstant")
  private List<FeedbackItem> buildNavigationIssueList() {
    List<FeedbackItem> list = new ArrayList<>();

    list.add(new FeedbackItem(getResources().getString(R.string.feedback_type_route_quality),
      R.drawable.ic_feedback_route_quality,
      FeedbackEvent.ROUTING_ERROR,
      EMPTY_FEEDBACK_DESCRIPTION));
    list.add(new FeedbackItem(getResources().getString(R.string.feedback_type_illegal_route),
      R.drawable.ic_feedback_illegal_route,
      FeedbackEvent.NOT_ALLOWED,
      EMPTY_FEEDBACK_DESCRIPTION));
    list.add(new FeedbackItem(getResources().getString(R.string.feedback_type_road_closure),
      R.drawable.ic_feedback_road_closure,
      FeedbackEvent.ROAD_CLOSED,
      EMPTY_FEEDBACK_DESCRIPTION));

    return list;
  }

  private FeedbackClickListener.ClickCallback navigationIssueClickCallback = new FeedbackClickListener.ClickCallback() {
    @Override
    public void onFeedbackItemClick(ImageView view, int feedbackPosition) {
      if (view != null) {
        view.setPressed(!view.isPressed());
      }
      FeedbackItem feedbackItem = notificationIssueAdapter.getFeedbackItem(feedbackPosition);
      feedbackBottomSheetListener.onFeedbackSelected(feedbackItem);
      startTimer();
    }
  };
}

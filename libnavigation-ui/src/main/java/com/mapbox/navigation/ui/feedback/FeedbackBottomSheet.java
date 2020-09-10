package com.mapbox.navigation.ui.feedback;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
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
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mapbox.navigation.ui.R;
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent;

import com.mapbox.navigation.ui.internal.utils.ViewUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
  private static final int GRID_SPAN_PORTRAIT = 3;
  private static final int GRID_SPAN_LANDSCAPE = 6;

  @Nullable
  private FeedbackBottomSheetListener feedbackBottomSheetListener;
  private TextView feedbackBottomSheetTitleText;
  private ImageButton cancelBtn;
  private RecyclerView feedbackCategories;
  private FeedbackAdapter feedbackCategoryAdapter;
  @Nullable
  private ProgressBar feedbackProgressBar;
  private ObjectAnimator countdownAnimation;
  private long duration;
  @Nullable
  private CountDownTimer timer = null;
  private Class<? extends FeedbackBottomSheetListener> feedbackBottomSheetListenerClass;
  @Nullable
  private DismissCommand dismissCommand = null;

  @NonNull
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
    return inflater.inflate(R.layout.mapbox_feedback_bottom_sheet, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    bind(view);
    initTitleTextView();
    initButtons();
    initFeedbackRecyclerView();
    initCountDownAnimation();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
    dialog.setOnShowListener(dialog1 -> {
      BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog1;
      FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
      if (bottomSheet != null) {
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
      }
    });
    return dialog;
  }

  @Override
  public void onDismiss(@NotNull DialogInterface dialog) {
    super.onDismiss(dialog);
    if (feedbackBottomSheetListener != null) {
      feedbackBottomSheetListener.onFeedbackDismissed();
    }
  }

  @Override
  public void onDestroyView() {
    removeListener();
    removeDialogDismissMessage();
    cancelCountdownAnimation();
    super.onDestroyView();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (feedbackBottomSheetListener != null) {
      feedbackBottomSheetListenerClass = feedbackBottomSheetListener.getClass();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (dismissCommand != null) {
      dismissCommand.invoke();
    }
    dismissCommand = null;
  }

  @Override
  public void onAnimationEnd(Animator animation) {
    if (FeedbackBottomSheet.this.isResumed()) {
      FeedbackBottomSheet.this.dismissWithoutSelection();
    } else {
      dismissCommand = delayedDismissCommand;
    }
  }

  @Override
  public void onCancel(@NonNull DialogInterface dialog) {
    super.onCancel(dialog);
    if (feedbackBottomSheetListener != null) {
      feedbackBottomSheetListener.onFeedbackSelected(null);
    }
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
    if (feedbackBottomSheetListenerClass == null || feedbackBottomSheetListenerClass.isInstance(feedbackBottomSheetListener)) {
      this.feedbackBottomSheetListener = feedbackBottomSheetListener;
    }
  }

  /**
   * @param duration in milliseconds, the BottomSheet will show before being dismissed.
   */
  public void setDuration(long duration) {
    this.duration = duration;
  }

  private void bind(@NonNull View bottomSheetView) {
    feedbackBottomSheetTitleText = bottomSheetView.findViewById(R.id.feedbackBottomSheetTitleText);
    cancelBtn = bottomSheetView.findViewById(R.id.cancelBtn);
    cancelBtn.setColorFilter(Color.WHITE);

    feedbackCategories = bottomSheetView.findViewById(R.id.feedbackCategories);

    feedbackProgressBar = bottomSheetView.findViewById(R.id.feedbackProgress);
  }

  private void initTitleTextView() {
    feedbackBottomSheetTitleText.setText(R.string.mapbox_report_feedback);
  }

  private void initButtons() {
    cancelBtn.setOnClickListener(view -> dismissWithoutSelection());
  }

  private void initFeedbackRecyclerView() {
    final Context context = requireContext();

    feedbackCategoryAdapter = new FeedbackAdapter(buildFeedbackCategoryList());
    feedbackCategories.setAdapter(feedbackCategoryAdapter);
    feedbackCategories.setOverScrollMode(RecyclerView.OVER_SCROLL_IF_CONTENT_SCROLLS);
    feedbackCategories.addOnItemTouchListener(new FeedbackClickListener(context, feedbackClickListener));
    if (ViewUtils.isLandscape(context)) {
      feedbackCategories.setLayoutManager(new GridLayoutManager(context, GRID_SPAN_LANDSCAPE));
    } else {
      feedbackCategories.setLayoutManager(new GridLayoutManager(context, GRID_SPAN_PORTRAIT));
    }
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

  private void dismissWithoutSelection() {
    if (feedbackBottomSheetListener != null) {
      feedbackBottomSheetListener.onFeedbackSelected(null);
    }
    dismiss();
  }

  @NonNull
  @SuppressLint("WrongConstant")
  private List<FeedbackItem> buildFeedbackCategoryList() {
    List<FeedbackItem> list = new ArrayList<>();

    list.add(new FeedbackItem(
            FeedbackHelper.INSTANCE.getFeedbackText(
                    FeedbackEvent.INCORRECT_VISUAL_GUIDANCE,
                    Objects.requireNonNull(getContext())
            ),
            FeedbackHelper.INSTANCE.getFeedbackImage(FeedbackEvent.INCORRECT_VISUAL_GUIDANCE),
            FeedbackEvent.INCORRECT_VISUAL_GUIDANCE,
            EMPTY_FEEDBACK_DESCRIPTION)
    );
    list.add(new FeedbackItem(
            FeedbackHelper.INSTANCE.getFeedbackText(
                    FeedbackEvent.INCORRECT_AUDIO_GUIDANCE,
                    Objects.requireNonNull(getContext())
            ),
            FeedbackHelper.INSTANCE.getFeedbackImage(FeedbackEvent.INCORRECT_AUDIO_GUIDANCE),
            FeedbackEvent.INCORRECT_AUDIO_GUIDANCE,
            EMPTY_FEEDBACK_DESCRIPTION)
    );
    list.add(new FeedbackItem(
            FeedbackHelper.INSTANCE.getFeedbackText(
                    FeedbackEvent.POSITIONING_ISSUE,
                    Objects.requireNonNull(getContext())
            ),
            FeedbackHelper.INSTANCE.getFeedbackImage(FeedbackEvent.POSITIONING_ISSUE),
            FeedbackEvent.POSITIONING_ISSUE,
            EMPTY_FEEDBACK_DESCRIPTION)
    );
    list.add(new FeedbackItem(
            FeedbackHelper.INSTANCE.getFeedbackText(
                    FeedbackEvent.ROUTING_ERROR,
                    Objects.requireNonNull(getContext())
            ),
            FeedbackHelper.INSTANCE.getFeedbackImage(FeedbackEvent.ROUTING_ERROR),
            FeedbackEvent.ROUTING_ERROR,
            EMPTY_FEEDBACK_DESCRIPTION)
    );
    list.add(new FeedbackItem(
            FeedbackHelper.INSTANCE.getFeedbackText(
                    FeedbackEvent.NOT_ALLOWED,
                    Objects.requireNonNull(getContext())
            ),
            FeedbackHelper.INSTANCE.getFeedbackImage(FeedbackEvent.NOT_ALLOWED),
            FeedbackEvent.NOT_ALLOWED,
            EMPTY_FEEDBACK_DESCRIPTION)
    );
    list.add(new FeedbackItem(
            FeedbackHelper.INSTANCE.getFeedbackText(
                    FeedbackEvent.ROAD_CLOSED,
                    Objects.requireNonNull(getContext())
            ),
            FeedbackHelper.INSTANCE.getFeedbackImage(FeedbackEvent.ROAD_CLOSED),
            FeedbackEvent.ROAD_CLOSED,
            EMPTY_FEEDBACK_DESCRIPTION)
    );

    return list;
  }

  @NonNull
  private FeedbackClickListener.ClickCallback feedbackClickListener = new FeedbackClickListener.ClickCallback() {
    @Override
    public void onFeedbackItemClick(@Nullable ImageView view, int feedbackPosition) {
      if (view != null) {
        view.setPressed(!view.isPressed());
      }

      FeedbackItem feedbackItem = feedbackCategoryAdapter.getFeedbackItem(feedbackPosition);
      if (feedbackBottomSheetListener != null) {
        feedbackBottomSheetListener.onFeedbackSelected(feedbackItem);
      }
      startTimer();
    }
  };

  @IntDef( {FEEDBACK_FLOW_IDLE, FEEDBACK_FLOW_SENT, FEEDBACK_FLOW_CANCEL})
  @Retention(RetentionPolicy.SOURCE)
  public @interface FeedbackFlowStatus {
  }

  /**
   * FEEDBACK_FLOW_IDLE means the {@link FeedbackBottomSheet} is currently shown
   * and is waiting for user's selection.
   */
  public static final int FEEDBACK_FLOW_IDLE = -1;

  /**
   * FEEDBACK_FLOW_SENT means the user has selected a {@link FeedbackEvent.Type}
   * and the {@link FeedbackBottomSheet} dismisses.
   */
  public static final int FEEDBACK_FLOW_SENT = 0;

  /**
   * FEEDBACK_FLOW_CANCEL means the {@link FeedbackBottomSheet} dismisses.
   * User doesn't select any {@link FeedbackEvent.Type}.
   */
  public static final int FEEDBACK_FLOW_CANCEL = 2;

  private interface DismissCommand {
    void invoke();
  }

  private DismissCommand delayedDismissCommand = FeedbackBottomSheet.this::dismissWithoutSelection;
}

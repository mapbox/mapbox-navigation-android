package com.mapbox.navigation.ui.feedback;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mapbox.libnavigation.ui.R;
import com.mapbox.navigation.ui.ThemeSwitcher;

public class FeedbackBottomSheet extends BottomSheetDialogFragment implements FeedbackClickListener.ClickCallback,
  Animator.AnimatorListener {

  public static final String TAG = FeedbackBottomSheet.class.getSimpleName();
  private static final long CLOSE_BOTTOM_SHEET_AFTER = 150L;
  private static final long TIMER_INTERVAL = 1L;
  private static final int LANDSCAPE_GRID_SPAN = 4;
  private static final int PORTRAIT_GRID_SPAN = 2;

  private FeedbackBottomSheetListener feedbackBottomSheetListener;
  private FeedbackAdapter feedbackAdapter;
  private RecyclerView feedbackItems;
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
    return inflater.inflate(R.layout.feedback_bottom_sheet_layout, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    bind(view);
    initFeedbackRecyclerView();
    initCountDownAnimation();
    initBackground(view);
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
  public void onFeedbackItemClick(ImageView imageView, int feedbackPosition) {
    if (imageView != null) {
      imageView.setPressed(!imageView.isPressed());
    }
    FeedbackItem feedbackItem = feedbackAdapter.getFeedbackItem(feedbackPosition);
    feedbackBottomSheetListener.onFeedbackSelected(feedbackItem);
    startTimer();
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
    feedbackItems = bottomSheetView.findViewById(R.id.feedbackItems);
    feedbackProgressBar = bottomSheetView.findViewById(R.id.feedbackProgress);
  }

  private void initFeedbackRecyclerView() {
    Context context = getContext();
    feedbackAdapter = new FeedbackAdapter(context);
    feedbackItems.setAdapter(feedbackAdapter);
    feedbackItems.setOverScrollMode(RecyclerView.OVER_SCROLL_IF_CONTENT_SCROLLS);
    feedbackItems.addOnItemTouchListener(new FeedbackClickListener(context, this));
    if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      feedbackItems.setLayoutManager(new GridLayoutManager(context, LANDSCAPE_GRID_SPAN));
    } else {
      feedbackItems.setLayoutManager(new GridLayoutManager(context, PORTRAIT_GRID_SPAN));
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

  private void initBackground(View view) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
      int navigationViewPrimaryColor = ThemeSwitcher.retrieveThemeColor(getContext(),
        R.attr.navigationViewPrimary);
      int navigationViewSecondaryColor = ThemeSwitcher.retrieveThemeColor(getContext(),
        R.attr.navigationViewSecondary);
      // BottomSheet background
      Drawable bottomSheetBackground = DrawableCompat.wrap(view.getBackground()).mutate();
      DrawableCompat.setTint(bottomSheetBackground, navigationViewPrimaryColor);
      // ProgressBar progress color
      LayerDrawable progressBarBackground = (LayerDrawable) feedbackProgressBar.getProgressDrawable();
      Drawable progressDrawable = progressBarBackground.getDrawable(1);
      progressDrawable.setColorFilter(navigationViewSecondaryColor, PorterDuff.Mode.SRC_IN);
    }
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
}

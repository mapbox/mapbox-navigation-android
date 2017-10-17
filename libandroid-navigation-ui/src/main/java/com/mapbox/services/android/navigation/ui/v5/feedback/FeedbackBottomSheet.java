package com.mapbox.services.android.navigation.ui.v5.feedback;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

import com.mapbox.services.android.navigation.ui.v5.R;

public class FeedbackBottomSheet extends BottomSheetDialogFragment {

  public static final String TAG = FeedbackBottomSheet.class.getSimpleName();

  private RecyclerView feedbackItems;
  private ProgressBar feedbackProgressBar;

  public static FeedbackBottomSheet newInstance() {
    return new FeedbackBottomSheet();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(BottomSheetDialogFragment.STYLE_NO_FRAME, R.style.Theme_Design_BottomSheetDialog);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View bottomSheetView = inflater.inflate(R.layout.feedback_bottom_sheet_layout, container, false);
    bind(bottomSheetView);
    initFeedbackRecyclerView();
    initCountDownAnimation();
    return bottomSheetView;
  }

  private void bind(View bottomSheetView) {
    feedbackItems = bottomSheetView.findViewById(R.id.feedbackItems);
    feedbackProgressBar = bottomSheetView.findViewById(R.id.feedbackProgress);
  }

  private void initFeedbackRecyclerView() {
    feedbackItems.setAdapter(new FeedbackAdapter());
    feedbackItems.setLayoutManager(new GridLayoutManager(getContext(), 3));
  }

  private void initCountDownAnimation() {
    ObjectAnimator countdownAnimation = ObjectAnimator.ofInt(feedbackProgressBar,
      "progress", 0);
    countdownAnimation.setInterpolator(new LinearInterpolator());
    countdownAnimation.setDuration(5000);
    countdownAnimation.addListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animation) {

      }

      @Override
      public void onAnimationEnd(Animator animation) {
        FeedbackBottomSheet.this.dismiss();
      }

      @Override
      public void onAnimationCancel(Animator animation) {

      }

      @Override
      public void onAnimationRepeat(Animator animation) {

      }
    });
    countdownAnimation.start();
  }
}

package com.mapbox.services.android.navigation.ui.v5.alert;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.ThemeSwitcher;

/**
 * A custom View that can show a quick message to a user.
 * <p>
 * Accompanied with a set duration (in millis), the View will automatically dismiss itself
 * after the duration countdown has completed.
 */
public class AlertView extends CardView {

  private TextView alertText;
  private ProgressBar alertProgressBar;

  private Animation fadeOut;
  private Animation slideDownTop;
  private ObjectAnimator countdownAnimation;

  public AlertView(Context context) {
    this(context, null);
  }

  public AlertView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public AlertView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
    initAnimations();
    initBackground();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (countdownAnimation != null) {
      countdownAnimation.cancel();
    }
  }

  /**
   * Animates the View from top down to its set position.
   * <p>
   * If a non-null duration is passed, a countdown loading bar will show to
   * indicate the View will be automatically dismissed if not clicked.
   *
   * @param alertText   text to be shown in the View
   * @param duration    in milliseconds, how long the view will be shown
   * @param showLoading true if should show the progress bar, false if not
   */
  public void show(String alertText, long duration, boolean showLoading) {
    this.alertText.setText(alertText);
    alertProgressBar.setProgress(alertProgressBar.getMax());
    // Start animation based on current visibility
    if (getVisibility() == INVISIBLE) {
      setVisibility(VISIBLE);
      startAnimation(slideDownTop);

      // If a duration is found, start the countdown
      if (duration > 0L) {
        startCountdown(duration);
      }
      // Show / hide loading
      showLoading(showLoading);
    }
  }

  /**
   * Hides the View with a slide up animation if the View is currently VISIBLE.
   *
   * @since 0.7.0
   */
  public void hide() {
    if (getVisibility() == VISIBLE) {
      startAnimation(fadeOut);
      setVisibility(INVISIBLE);
    }
  }

  /**
   * Returns the current text being shown by the {@link AlertView}.
   *
   * @return current text in alertText {@link TextView}
   * @since 0.7.0
   */
  public String getAlertText() {
    return alertText.getText().toString();
  }

  private void init() {
    inflate(getContext(), R.layout.alert_view_layout, this);
  }

  private void bind() {
    alertText = findViewById(R.id.alertText);
    alertProgressBar = findViewById(R.id.alertProgressBar);
  }

  private void initAnimations() {
    fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setInterpolator(new AccelerateInterpolator());
    fadeOut.setDuration(300);
    slideDownTop = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down_top);
    slideDownTop.setInterpolator(new OvershootInterpolator(2.0f));
  }

  private void initBackground() {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
      int progressColor = ThemeSwitcher.retrieveThemeColor(getContext(),
        R.attr.navigationViewProgress);
      int progressBackgroundColor = ThemeSwitcher.retrieveThemeColor(getContext(),
        R.attr.navigationViewProgressBackground);

      LayerDrawable progressBarDrawable = (LayerDrawable) alertProgressBar.getProgressDrawable();
      // ProgressBar progress color
      Drawable progressBackgroundDrawable = progressBarDrawable.getDrawable(0);
      progressBackgroundDrawable.setColorFilter(progressBackgroundColor, PorterDuff.Mode.SRC_IN);


      // ProgressBar background color
      Drawable progressDrawable = progressBarDrawable.getDrawable(1);
      progressDrawable.setColorFilter(progressColor, PorterDuff.Mode.SRC_IN);

      // Hide the background
      getBackground().setAlpha(0);
    } else {
      setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.transparent));
    }
  }

  private void startCountdown(long duration) {
    countdownAnimation = ObjectAnimator.ofInt(alertProgressBar,
      "progress", 0);
    countdownAnimation.setInterpolator(new LinearInterpolator());
    countdownAnimation.setDuration(duration);
    countdownAnimation.addListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animation) {

      }

      @Override
      public void onAnimationEnd(Animator animation) {
        hide();
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

  private void showLoading(boolean showLoading) {
    alertProgressBar.setVisibility(showLoading ? VISIBLE : INVISIBLE);
  }
}

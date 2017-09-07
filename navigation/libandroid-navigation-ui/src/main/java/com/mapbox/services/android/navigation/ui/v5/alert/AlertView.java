package com.mapbox.services.android.navigation.ui.v5.alert;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.mapbox.services.android.navigation.ui.v5.R;

public class AlertView extends LinearLayout {

  private static final int LOADING_LAYOUT = 0;
  private static final int SUCCESS_LAYOUT = 1;
  private static final int ERROR_LAYOUT = 2;
  private static final int WARNING_LAYOUT = 3;

  ViewFlipper alertFlipper;
  TextView loadingText;
  TextView successText;
  TextView errorText;
  TextView warningText;
  LinearLayout tryAgainLayout;

  private Animation fadeIn;
  private Animation fadeOut;
  private Animation slideUpTop;
  private Animation slideUpTopDelay;
  private Animation slideDownTop;

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
    initAnimations();
    initFlipper();
  }

  public void showLoadingAlert(String loadingText) {
    if (alertFlipper.getDisplayedChild() != LOADING_LAYOUT) {
      alertFlipper.setDisplayedChild(LOADING_LAYOUT);
    }
    this.loadingText.setText(loadingText);
    show();
  }

  public void showSuccessAlert(String successText) {
    if (alertFlipper.getDisplayedChild() != SUCCESS_LAYOUT) {
      alertFlipper.setDisplayedChild(SUCCESS_LAYOUT);
    }
    this.successText.setText(successText);
    show();
  }

  public void showWarningAlert(String warningText) {
    if (alertFlipper.getDisplayedChild() != WARNING_LAYOUT) {
      alertFlipper.setDisplayedChild(WARNING_LAYOUT);
    }
    this.warningText.setText(warningText);
    show();
  }

  public void hide() {
    if (this.getVisibility() == VISIBLE) {
      this.startAnimation(slideUpTop);
      this.setVisibility(INVISIBLE);
    }
  }

  private void init() {
    inflate(getContext(), R.layout.alert_view_layout, this);
  }

  private void initAnimations() {
    fadeIn = new AlphaAnimation(0, 1);
    fadeIn.setInterpolator(new DecelerateInterpolator());
    fadeIn.setDuration(300);

    fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setInterpolator(new AccelerateInterpolator());
    fadeOut.setDuration(300);

    slideUpTop = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up_alert);
    slideDownTop = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down_alert);
    slideDownTop.setInterpolator(new OvershootInterpolator(2.0f));
  }

  private void initFlipper() {
    alertFlipper.setInAnimation(fadeIn);
    alertFlipper.setOutAnimation(fadeOut);
  }

  private void show() {
    if (this.getVisibility() == INVISIBLE) {
      this.setVisibility(VISIBLE);
      this.startAnimation(slideDownTop);
    }
  }
}

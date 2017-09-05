package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;

public class RecenterButton extends LinearLayout {

  private Animation slideUpBottom;

  public RecenterButton(Context context) {
    this(context, null);
  }

  public RecenterButton(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public RecenterButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  public void show() {
    setVisibility(VISIBLE);
    startAnimation(slideUpBottom);
  }

  public void hide() {
    setVisibility(INVISIBLE);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    initAnimation();
  }

  private void init() {
    inflate(getContext(), R.layout.recenter_btn_layout, this);
  }

  private void initAnimation() {
    slideUpBottom = new TranslateAnimation(0f, 0f, 125f, 0f);
    slideUpBottom.setDuration(300);
    slideUpBottom.setInterpolator(new OvershootInterpolator(2.0f));
  }
}

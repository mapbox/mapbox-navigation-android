package com.mapbox.services.android.navigation.ui.v5;

import android.animation.Animator;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.widget.LinearLayout;

public class RecenterButton extends LinearLayout {

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

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    initListener();
  }

  private void init() {
    inflate(getContext(), R.layout.recenter_btn_layout, this);
  }

  private void initListener() {
    setOnTouchListener(new OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        hide(view, motionEvent);
        return false;
      }
    });
  }

  private void hide(View view, MotionEvent motionEvent) {
    int cx = view.getWidth();
    int cy = view.getHeight();
    float initialRadius = (float) Math.hypot(cx, cy);
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      startHideAnimation(view, motionEvent, initialRadius);
    } else {
      setVisibility(INVISIBLE);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void startHideAnimation(View view, MotionEvent motionEvent, float initialRadius) {
    Animator hide = ViewAnimationUtils.createCircularReveal(view,
      (int) motionEvent.getX(), (int) motionEvent.getY(), initialRadius, 0);
    hide.setDuration(150);
    hide.setInterpolator(new AccelerateInterpolator());
    hide.addListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animator) {

      }

      @Override
      public void onAnimationEnd(Animator animator) {
        setVisibility(INVISIBLE);
      }

      @Override
      public void onAnimationCancel(Animator animator) {

      }

      @Override
      public void onAnimationRepeat(Animator animator) {

      }
    });
    hide.start();
  }
}

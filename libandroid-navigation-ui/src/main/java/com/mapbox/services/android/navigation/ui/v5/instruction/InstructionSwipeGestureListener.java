package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.view.GestureDetector;
import android.view.MotionEvent;

public abstract class InstructionSwipeGestureListener extends GestureDetector.SimpleOnGestureListener {

  public abstract boolean onTouch();

  public abstract boolean onSwipeLeft();

  public abstract boolean onSwipeRight();

  @Override
  public boolean onSingleTapConfirmed(MotionEvent event) {
    return onTouch();
  }

  @Override
  public boolean onFling(MotionEvent start, MotionEvent end, float vx, float vy) {
    if (start.getX() < end.getX()) {
      return onSwipeRight();
    }

    if (start.getX() > end.getX()) {
      return onSwipeLeft();
    }

    return false;
  }
}

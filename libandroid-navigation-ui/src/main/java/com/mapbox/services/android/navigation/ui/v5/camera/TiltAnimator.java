package com.mapbox.services.android.navigation.ui.v5.camera;

import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;

public class TiltAnimator extends ValueAnimator {

  private float targetTilt;

  public TiltAnimator(double targetTilt, long duration) {
    setEvaluator(new FloatEvaluator());
    setDuration(duration);
    this.targetTilt = (float) targetTilt;
  }

  public float getTargetTilt() {
    return targetTilt;
  }
}

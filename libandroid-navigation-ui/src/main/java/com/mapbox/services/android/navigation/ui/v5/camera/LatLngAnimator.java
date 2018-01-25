package com.mapbox.services.android.navigation.ui.v5.camera;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.support.annotation.NonNull;

import com.mapbox.mapboxsdk.geometry.LatLng;

public class LatLngAnimator extends ValueAnimator {

  private LatLng target;

  public LatLngAnimator(@NonNull LatLng target, long duration) {
    setDuration(duration);
    this.target = target;
  }

  @Override
  public void setObjectValues(Object... values) {
    super.setObjectValues(values);
    setEvaluator(new LatLngEvaluator());
  }

  public LatLng getTarget() {
    return target;
  }

  private static class LatLngEvaluator implements TypeEvaluator<LatLng> {

    private final LatLng latLng = new LatLng();

    @Override
    public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
      latLng.setLatitude(startValue.getLatitude()
        + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
      latLng.setLongitude(startValue.getLongitude()
        + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
      return latLng;
    }
  }
}

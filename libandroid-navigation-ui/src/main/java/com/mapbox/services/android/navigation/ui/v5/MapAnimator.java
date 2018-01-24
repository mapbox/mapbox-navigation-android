package com.mapbox.services.android.navigation.ui.v5;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.ArrayList;
import java.util.List;

public class MapAnimator {

  private List<Animator> animators;
  private AnimatorSet animatorSet;

  private MapAnimator(List<Animator> animators) {
    this.animators = animators;
    animatorSet = new AnimatorSet();
  }

  public void playTogether() {
    animatorSet.playTogether(animators);
    animatorSet.start();
  }

  public void playSequentially() {
    animatorSet.playSequentially(animators);
    animatorSet.start();
  }

  public void playTogether(@Nullable Animator.AnimatorListener listener) {
    animatorSet.addListener(listener);
    playTogether();
  }

  public void playSequentially(@Nullable Animator.AnimatorListener listener) {
    animatorSet.addListener(listener);
    playSequentially();
  }

  public void cancel() {
    animatorSet.cancel();
  }

  public static Builder builder(MapboxMap mapboxMap) {
    return new Builder(mapboxMap);
  }

  public static final class Builder {

    private final MapboxMap mapboxMap;
    private List<Animator> animators;
    private CameraPosition currentPosition;

    private Builder(MapboxMap mapboxMap) {
      this.mapboxMap = mapboxMap;
      this.animators = new ArrayList<>();
      // Get the current target from the current map camera position
      currentPosition = mapboxMap.getCameraPosition();
    }

    public Builder addLatLngAnimator(@NonNull LatLng target, @Nullable TimeInterpolator interpolator, long duration) {

      LatLng currentTarget = currentPosition.target;

      if (currentTarget == target) {
        return this;
      }

      ValueAnimator latLngAnimator = ValueAnimator.ofObject(new LatLngEvaluator(), currentTarget, target);
      latLngAnimator.setDuration(duration);

      if (interpolator != null) {
        latLngAnimator.setInterpolator(interpolator);
      }

      latLngAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          mapboxMap.setLatLng((LatLng) animation.getAnimatedValue());
        }
      });

      animators.add(latLngAnimator);

      return this;
    }

    public Builder addZoomAnimator(double targetZoom, @Nullable TimeInterpolator interpolator, long duration) {

      double currentZoom = currentPosition.zoom;

      if (currentZoom == targetZoom) {
        return this;
      }

      ValueAnimator zoomAnimator = ValueAnimator.ofFloat((float) currentZoom, (float) targetZoom);
      zoomAnimator.setDuration(duration);

      if (interpolator != null) {
        zoomAnimator.setInterpolator(interpolator);
      }

      zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          mapboxMap.setZoom((Float) animation.getAnimatedValue());
        }
      });

      animators.add(zoomAnimator);

      return this;
    }

    public Builder addBearingAnimator(double targetBearing, @Nullable TimeInterpolator interpolator, long duration) {

      float currentBearing = (float) currentPosition.bearing;

      float normalizedBearing = normalizeBearing(currentBearing, (float) targetBearing);

      if (currentBearing == normalizedBearing) {
        return this;
      }

      ValueAnimator bearingAnimator = ValueAnimator.ofFloat(currentBearing, normalizedBearing);
      bearingAnimator.setDuration(duration);

      if (interpolator != null) {
        bearingAnimator.setInterpolator(interpolator);
      }

      bearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          mapboxMap.setBearing((Float) animation.getAnimatedValue());
        }
      });

      animators.add(bearingAnimator);

      return this;
    }

    public Builder addTiltAnimator(float targetTilt, @Nullable TimeInterpolator interpolator, long duration) {

      float currentTilt = (float) currentPosition.tilt;

      if (currentTilt == targetTilt) {
        return this;
      }

      ValueAnimator tiltAnimator = ValueAnimator.ofFloat(currentTilt, targetTilt);
      tiltAnimator.setDuration(duration);

      if (interpolator != null) {
        tiltAnimator.setInterpolator(interpolator);
      }

      tiltAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          mapboxMap.setTilt((Float) animation.getAnimatedValue());
        }
      });

      animators.add(tiltAnimator);

      return this;
    }



    public MapAnimator build() {
      return new MapAnimator(animators);
    }
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

  private static float normalizeBearing(float currentBearing, float targetBearing) {
    double diff = currentBearing - targetBearing;
    if (diff > 180.0f) {
      targetBearing += 360.0f;
    } else if (diff < -180.0f) {
      targetBearing -= 360.f;
    }
    return targetBearing;
  }
}

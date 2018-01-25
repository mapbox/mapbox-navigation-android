package com.mapbox.services.android.navigation.ui.v5.camera;

import android.animation.Animator;
import android.animation.AnimatorSet;
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

  public void playTogether(@Nullable Animator.AnimatorListener listener) {
    animatorSet.addListener(listener);
    playTogether();
  }

  public void playSequentially() {
    animatorSet.playSequentially(animators);
    animatorSet.start();
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

    public Builder addLatLngAnimator(@NonNull LatLngAnimator latLngAnimator) {

      LatLng currentTarget = currentPosition.target;
      if (currentTarget == latLngAnimator.getTarget()) {
        return this;
      }

      latLngAnimator.setObjectValues(currentTarget, latLngAnimator.getTarget());
      latLngAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          mapboxMap.setLatLng((LatLng) animation.getAnimatedValue());
        }
      });

      animators.add(latLngAnimator);
      return this;
    }

    public Builder addZoomAnimator(@NonNull ZoomAnimator zoomAnimator) {

      float currentZoom = (float) currentPosition.zoom;
      if (currentZoom == zoomAnimator.getTargetZoom()) {
        return this;
      }

      zoomAnimator.setFloatValues(currentZoom, zoomAnimator.getTargetZoom());
      zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          mapboxMap.setZoom((Float) animation.getAnimatedValue());
        }
      });

      animators.add(zoomAnimator);
      return this;
    }

    public Builder addBearingAnimator(@NonNull BearingAnimator bearingAnimator) {

      float currentBearing = (float) currentPosition.bearing;
      float normalizedTargetBearing = normalizeBearing(currentBearing, bearingAnimator.getTargetBearing());
      if (currentBearing == normalizedTargetBearing) {
        return this;
      }

      bearingAnimator.setFloatValues(currentBearing, normalizedTargetBearing);
      bearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
          mapboxMap.setBearing((Float) animation.getAnimatedValue());
        }
      });

      animators.add(bearingAnimator);
      return this;
    }

    public Builder addTiltAnimator(@NonNull TiltAnimator tiltAnimator) {

      float currentTilt = (float) currentPosition.tilt;
      if (currentTilt == tiltAnimator.getTargetTilt()) {
        return this;
      }

      tiltAnimator.setFloatValues(currentTilt, tiltAnimator.getTargetTilt());
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

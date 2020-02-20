package com.mapbox.services.android.navigation.ui.v5.alert;

import android.animation.Animator;

import java.lang.ref.WeakReference;

class AlertViewAnimatorListener implements Animator.AnimatorListener {

  private final WeakReference<AlertView> alertViewWeakReference;

  AlertViewAnimatorListener(AlertView alertView) {
    this.alertViewWeakReference = new WeakReference<>(alertView);
  }

  @Override
  public void onAnimationStart(Animator animation) {
  }

  @Override
  public void onAnimationEnd(Animator animation) {
    hideAlertView();
  }

  @Override
  public void onAnimationCancel(Animator animation) {
  }

  @Override
  public void onAnimationRepeat(Animator animation) {
  }

  private void hideAlertView() {
    AlertView alertView = alertViewWeakReference.get();
    if (alertView != null) {
      alertView.hide();
    }
  }
}

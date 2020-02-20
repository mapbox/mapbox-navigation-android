package com.mapbox.services.android.navigation.ui.v5.alert;

import android.animation.Animator;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AlertViewAnimatorListenerTest {

  @Test
  public void onAnimationEnd_alertViewIsHidden() {
    AlertView alertView = mock(AlertView.class);
    AlertViewAnimatorListener listener = new AlertViewAnimatorListener(alertView);

    listener.onAnimationEnd(mock(Animator.class));

    verify(alertView).hide();
  }
}
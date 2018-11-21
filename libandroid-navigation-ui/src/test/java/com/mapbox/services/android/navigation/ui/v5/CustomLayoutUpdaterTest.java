package com.mapbox.services.android.navigation.ui.v5;

import android.support.v4.view.AsyncLayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class CustomLayoutUpdaterTest {

  @Test
  public void updateWithView_frameChildrenAreRemoved() {
    AsyncLayoutInflater inflater = mock(AsyncLayoutInflater.class);
    FrameLayout frame = mock(FrameLayout.class);
    View customView = mock(View.class);
    CustomLayoutUpdater layoutUpdater = new CustomLayoutUpdater(inflater);

    layoutUpdater.update(frame, customView);

    verify(frame).removeAllViews();
  }

  @Test
  public void updateWithView_nullCustomViewIgnored() {
    AsyncLayoutInflater inflater = mock(AsyncLayoutInflater.class);
    FrameLayout frame = mock(FrameLayout.class);
    View customView = null;
    CustomLayoutUpdater layoutUpdater = new CustomLayoutUpdater(inflater);

    layoutUpdater.update(frame, customView);

    verifyZeroInteractions(frame);
  }

  @Test
  public void updateWithView_customViewAddedToFrame() {
    AsyncLayoutInflater inflater = mock(AsyncLayoutInflater.class);
    FrameLayout frame = mock(FrameLayout.class);
    View customView = mock(View.class);
    CustomLayoutUpdater layoutUpdater = new CustomLayoutUpdater(inflater);

    layoutUpdater.update(frame, customView);

    verify(frame).addView(customView);
  }

  @Test
  public void updateWithResId_frameChildrenAreRemoved() {
    AsyncLayoutInflater inflater = mock(AsyncLayoutInflater.class);
    OnLayoutReplacedListener listener = mock(OnLayoutReplacedListener.class);
    FrameLayout frame = mock(FrameLayout.class);
    int layoutResId = 1234;
    CustomLayoutUpdater layoutUpdater = new CustomLayoutUpdater(inflater);

    layoutUpdater.update(frame, layoutResId, listener);

    verify(frame).removeAllViews();
  }

  @Test
  public void updateWithResId_inflaterReceivesResId() {
    AsyncLayoutInflater inflater = mock(AsyncLayoutInflater.class);
    OnLayoutReplacedListener listener = mock(OnLayoutReplacedListener.class);
    FrameLayout frame = mock(FrameLayout.class);
    int layoutResId = 1234;
    CustomLayoutUpdater layoutUpdater = new CustomLayoutUpdater(inflater);

    layoutUpdater.update(frame, layoutResId, listener);

    verify(inflater).inflate(
      eq(layoutResId), any(FrameLayout.class), any(AsyncLayoutInflater.OnInflateFinishedListener.class)
    );
  }
}
package com.mapbox.services.android.navigation.ui.v5;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class CustomLayoutInflateFinishedListenerTest {

  @Test
  public void onInflateFinished_nullFrameIgnored() {
    OnLayoutReplacedListener replacedListener = mock(OnLayoutReplacedListener.class);
    int someResId = 1234;
    ViewGroup nullFrame = null;
    CustomLayoutInflateFinishedListener listener = new CustomLayoutInflateFinishedListener(replacedListener);

    listener.onInflateFinished(mock(View.class), someResId, nullFrame);

    verifyZeroInteractions(replacedListener);
  }

  @Test
  public void onInflateFinished_customViewAdded() {
    OnLayoutReplacedListener replacedListener = mock(OnLayoutReplacedListener.class);
    View customView = mock(View.class);
    int someResId = 1234;
    FrameLayout frame = mock(FrameLayout.class);
    CustomLayoutInflateFinishedListener listener = new CustomLayoutInflateFinishedListener(replacedListener);

    listener.onInflateFinished(customView, someResId, frame);

    verify(frame).addView(customView);
  }

  @Test
  public void onInflateFinished_listenerIsTriggered() {
    OnLayoutReplacedListener replacedListener = mock(OnLayoutReplacedListener.class);
    View customView = mock(View.class);
    int someResId = 1234;
    FrameLayout frame = mock(FrameLayout.class);
    CustomLayoutInflateFinishedListener listener = new CustomLayoutInflateFinishedListener(replacedListener);

    listener.onInflateFinished(customView, someResId, frame);

    verify(replacedListener).onLayoutReplaced();
  }
}
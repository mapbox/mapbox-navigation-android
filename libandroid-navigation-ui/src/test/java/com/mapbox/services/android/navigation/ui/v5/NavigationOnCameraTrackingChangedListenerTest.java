package com.mapbox.services.android.navigation.ui.v5;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationOnCameraTrackingChangedListenerTest {

  @Test
  public void onCameraTrackingDismissed_presenterNotifiedWithVisibleBottomsheet() {
    NavigationPresenter presenter = mock(NavigationPresenter.class);
    BottomSheetBehavior behavior = mock(BottomSheetBehavior.class);
    when(behavior.getState()).thenReturn(BottomSheetBehavior.STATE_EXPANDED);
    NavigationOnCameraTrackingChangedListener listener = new NavigationOnCameraTrackingChangedListener(
      presenter, behavior
    );

    listener.onCameraTrackingDismissed();

    verify(presenter).onCameraTrackingDismissed();
  }

  @Test
  public void onCameraTrackingDismissed_ignoredWithHiddenBottomsheet() {
    NavigationPresenter presenter = mock(NavigationPresenter.class);
    BottomSheetBehavior behavior = mock(BottomSheetBehavior.class);
    when(behavior.getState()).thenReturn(BottomSheetBehavior.STATE_HIDDEN);
    NavigationOnCameraTrackingChangedListener listener = new NavigationOnCameraTrackingChangedListener(
      presenter, behavior
    );

    listener.onCameraTrackingDismissed();

    verify(presenter, times(0)).onCameraTrackingDismissed();
  }
}
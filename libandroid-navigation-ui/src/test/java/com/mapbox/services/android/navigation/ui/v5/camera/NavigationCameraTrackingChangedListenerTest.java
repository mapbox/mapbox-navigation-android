package com.mapbox.services.android.navigation.ui.v5.camera;

import com.mapbox.mapboxsdk.location.modes.CameraMode;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationCameraTrackingChangedListenerTest {

  @Test
  public void onCameraTrackingDismissed_cameraSetToTrackingNone() {
    NavigationCamera camera = mock(NavigationCamera.class);
    NavigationCameraTrackingChangedListener listener = new NavigationCameraTrackingChangedListener(camera);

    listener.onCameraTrackingDismissed();

    verify(camera).updateCameraTrackingMode(eq(NavigationCamera.NAVIGATION_TRACKING_MODE_NONE));
  }

  @Test
  public void onCameraTrackingChanged_navigationCameraTrackingUpdated() {
    NavigationCamera camera = mock(NavigationCamera.class);
    when(camera.findTrackingModeFor(CameraMode.TRACKING_GPS)).thenReturn(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS);
    NavigationCameraTrackingChangedListener listener = new NavigationCameraTrackingChangedListener(camera);

    listener.onCameraTrackingChanged(CameraMode.TRACKING_GPS);

    verify(camera).updateCameraTrackingMode(eq(NavigationCamera.NAVIGATION_TRACKING_MODE_GPS));
  }
}
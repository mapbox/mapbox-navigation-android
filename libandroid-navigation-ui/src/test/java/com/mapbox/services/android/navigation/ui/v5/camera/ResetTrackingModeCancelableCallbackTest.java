package com.mapbox.services.android.navigation.ui.v5.camera;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ResetTrackingModeCancelableCallbackTest {

  @Test
  public void onCancel_trackingModeIsSet() {
    NavigationCamera camera = mock(NavigationCamera.class);
    int trackingMode = NavigationCamera.NAVIGATION_TRACKING_MODE_GPS;
    ResetTrackingModeCancelableCallback callback = new ResetTrackingModeCancelableCallback(camera, trackingMode);

    callback.onCancel();

    verify(camera).updateCameraTrackingMode(eq(trackingMode));
  }

  @Test
  public void onFinish_trackingModeIsSet() {
    NavigationCamera camera = mock(NavigationCamera.class);
    int trackingMode = NavigationCamera.NAVIGATION_TRACKING_MODE_GPS;
    ResetTrackingModeCancelableCallback callback = new ResetTrackingModeCancelableCallback(camera, trackingMode);

    callback.onFinish();

    verify(camera).updateCameraTrackingMode(eq(trackingMode));
  }
}
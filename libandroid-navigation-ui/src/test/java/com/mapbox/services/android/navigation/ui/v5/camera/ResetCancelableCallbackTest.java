package com.mapbox.services.android.navigation.ui.v5.camera;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ResetCancelableCallbackTest {

  @Test
  public void onFinish_dynamicCameraIsReset() {
    NavigationCamera camera = mock(NavigationCamera.class);
    ResetCancelableCallback callback = new ResetCancelableCallback(camera);

    callback.onFinish();

    verify(camera).updateIsResetting(eq(false));
  }

  @Test
  public void onCancel_dynamicCameraIsReset() {
    NavigationCamera camera = mock(NavigationCamera.class);
    ResetCancelableCallback callback = new ResetCancelableCallback(camera);

    callback.onCancel();

    verify(camera).updateIsResetting(eq(false));
  }
}
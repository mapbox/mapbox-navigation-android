package com.mapbox.navigation.ui;

import com.mapbox.navigation.ui.NavigationViewOfflineRouter;
import com.mapbox.navigation.ui.OfflineRouterConfiguredCallback;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OfflineRouterConfiguredCallbackTest {

  @Test
  public void onConfigured_configuredIsSetToTrue() {
    NavigationViewOfflineRouter offlineRouter = mock(NavigationViewOfflineRouter.class);
    OfflineRouterConfiguredCallback callback = new OfflineRouterConfiguredCallback(offlineRouter);

    callback.onConfigured(122);

    verify(offlineRouter).setIsConfigured(eq(true));
  }
}
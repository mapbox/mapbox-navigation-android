package com.mapbox.navigation.ui;

import com.mapbox.navigation.ui.MapConnectivityController;
import com.mapbox.navigation.ui.RegionDownloadCallback;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RegionDownloadCallbackTest {

  @Test
  public void onComplete_disconnectStateIsAssigned() {
    MapConnectivityController mapConnectivityController = mock(MapConnectivityController.class);
    RegionDownloadCallback callback = new RegionDownloadCallback(mapConnectivityController);

    callback.onComplete();

    verify(mapConnectivityController).assign(eq(false));
  }

  @Test
  public void onError_disconnectStateIsAssigned() {
    MapConnectivityController mapConnectivityController = mock(MapConnectivityController.class);
    RegionDownloadCallback callback = new RegionDownloadCallback(mapConnectivityController);

    callback.onError("some error message");

    verify(mapConnectivityController).assign(eq(false));

  }
}
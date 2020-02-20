package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.mapboxsdk.offline.OfflineRegion;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CreateOfflineRegionCallbackTest {

  @Test
  public void checksOfflineRegionDownloadStateSetToActiveWhenOnCreate() {
    OfflineRegionDownloadCallback mockedOfflineRegionDownloadCallback = mock(OfflineRegionDownloadCallback.class);
    OfflineRegion mockedOfflineRegion = mock(OfflineRegion.class);
    CreateOfflineRegionCallback theOfflineRegionCallback =
      new CreateOfflineRegionCallback(mockedOfflineRegionDownloadCallback);

    theOfflineRegionCallback.onCreate(mockedOfflineRegion);

    verify(mockedOfflineRegion).setDownloadState(eq(OfflineRegion.STATE_ACTIVE));
  }

  @Test
  public void checksOfflineRegionObserverIsSetWhenOnCreate() {
    OfflineRegionDownloadCallback mockedOfflineRegionDownloadCallback = mock(OfflineRegionDownloadCallback.class);
    OfflineRegion mockedOfflineRegion = mock(OfflineRegion.class);
    CreateOfflineRegionCallback theOfflineRegionCallback =
      new CreateOfflineRegionCallback(mockedOfflineRegionDownloadCallback);

    theOfflineRegionCallback.onCreate(mockedOfflineRegion);

    verify(mockedOfflineRegion).setObserver(any(OfflineRegion.OfflineRegionObserver.class));
  }

  @Test
  public void checksOnErrorCallbackIsCalledWhenOnError() {
    OfflineRegionDownloadCallback mockedOfflineRegionDownloadCallback = mock(OfflineRegionDownloadCallback.class);
    CreateOfflineRegionCallback theOfflineRegionCallback =
      new CreateOfflineRegionCallback(mockedOfflineRegionDownloadCallback);
    String anErrorMessage = "an error message";

    theOfflineRegionCallback.onError(anErrorMessage);

    verify(mockedOfflineRegionDownloadCallback).onError(eq(anErrorMessage));
  }
}
package com.mapbox.services.android.navigation.ui.v5;

import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OfflineRegionObserverTest {

  @Test
  public void onStatusChanged_completeCallbackIsTriggeredWithCompleteStatus() {
    OfflineRegionDownloadCallback callback = mock(OfflineRegionDownloadCallback.class);
    OfflineRegionStatus status = mock(OfflineRegionStatus.class);
    when(status.isComplete()).thenReturn(true);
    OfflineRegionObserver offlineRegionObserver = new OfflineRegionObserver(callback);

    offlineRegionObserver.onStatusChanged(status);

    verify(callback).onComplete();
  }

  @Test
  public void onError_errorCallbackIsTriggered() {
    OfflineRegionDownloadCallback callback = mock(OfflineRegionDownloadCallback.class);
    OfflineRegionError error = mock(OfflineRegionError.class);
    when(error.getMessage()).thenReturn("an error occurred");
    when(error.getReason()).thenReturn("because xyz");
    OfflineRegionObserver offlineRegionObserver = new OfflineRegionObserver(callback);

    offlineRegionObserver.onError(error);

    verify(callback).onError(eq("an error occurred because xyz"));
  }

  @Test
  public void mapboxTileCountLimitExceeded_errorCallbackIsTriggered() {
    OfflineRegionDownloadCallback callback = mock(OfflineRegionDownloadCallback.class);
    OfflineRegionObserver offlineRegionObserver = new OfflineRegionObserver(callback);

    offlineRegionObserver.mapboxTileCountLimitExceeded(6000L);

    verify(callback).onError(eq("Offline map tile limit reached 6000"));
  }
}
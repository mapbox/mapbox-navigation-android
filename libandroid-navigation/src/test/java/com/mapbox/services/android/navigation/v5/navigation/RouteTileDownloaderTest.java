package com.mapbox.services.android.navigation.v5.navigation;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RouteTileDownloaderTest {

  @Test
  public void startDownload_fetchRouteTilesIsCalled() {
    String tilePath = "some/path/";
    OfflineNavigator offlineNavigator = mock(OfflineNavigator.class);
    RouteTileDownloadListener listener = mock(RouteTileDownloadListener.class);
    OfflineTiles offlineTiles = mock(OfflineTiles.class);
    when(offlineTiles.version()).thenReturn("some-version");
    RouteTileDownloader downloader = new RouteTileDownloader(offlineNavigator, tilePath, listener);

    downloader.startDownload(offlineTiles);

    verify(offlineTiles).fetchRouteTiles(any(TarFetchedCallback.class));
  }

  @Test
  public void onError_downloadListenerErrorTriggered() {
    String tilePath = "some/path/";
    OfflineNavigator offlineNavigator = mock(OfflineNavigator.class);
    RouteTileDownloadListener listener = mock(RouteTileDownloadListener.class);
    OfflineError offlineError = mock(OfflineError.class);
    RouteTileDownloader downloader = new RouteTileDownloader(offlineNavigator, tilePath, listener);

    downloader.onError(offlineError);

    verify(listener).onError(offlineError);
  }
}
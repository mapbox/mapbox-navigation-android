package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

import org.junit.Test;

import java.io.File;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DownloadUpdateListenerTest {

  @Test
  public void onFinishedDownloading_tarIsUnpacked() {
    TileUnpacker tileUnpacker = mock(TileUnpacker.class);
    File file = mock(File.class);
    DownloadUpdateListener downloadUpdateListener = buildDownloadUpdateListener(tileUnpacker);

    downloadUpdateListener.onFinishedDownloading(file);

    verify(tileUnpacker).unpack(any(File.class), any(String.class), any(UnpackProgressUpdateListener.class));
  }

  @Test
  public void onErrorDownloading_offlineErrorIsSent() {
    RouteTileDownloader downloader = mock(RouteTileDownloader.class);
    DownloadUpdateListener downloadUpdateListener = buildDownloadUpdateListener(downloader);

    downloadUpdateListener.onErrorDownloading();

    verify(downloader).onError(any(OfflineError.class));
  }

  @NonNull
  private DownloadUpdateListener buildDownloadUpdateListener(TileUnpacker tileUnpacker) {
    RouteTileDownloader downloader = mock(RouteTileDownloader.class);
    String tilePath = "some/path/";
    String tileVersion = "some-version";
    RouteTileDownloadListener listener = mock(RouteTileDownloadListener.class);
    return new DownloadUpdateListener(
      downloader, tileUnpacker, tilePath, tileVersion, listener
    );
  }

  @NonNull
  private DownloadUpdateListener buildDownloadUpdateListener(RouteTileDownloader downloader) {
    TileUnpacker tileUnpacker = mock(TileUnpacker.class);
    String tilePath = "some/path/";
    String tileVersion = "some-version";
    RouteTileDownloadListener listener = mock(RouteTileDownloadListener.class);
    return new DownloadUpdateListener(
      downloader, tileUnpacker, tilePath, tileVersion, listener
    );
  }
}
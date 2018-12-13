package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.utils.DownloadTask;

/**
 * This class serves to contain the complicated chain of events that must happen to download
 * offline routing tiles. It creates and maintains a directory structure with the root in the
 * Offline directory, or wherever someone specifies.
 */
class RouteTileDownloader {

  private static final String FILE_EXTENSION_TAR = "tar";
  private final OfflineNavigator offlineNavigator;
  private final String tilePath;
  private final RouteTileDownloadListener listener;

  RouteTileDownloader(OfflineNavigator offlineNavigator, String tilePath, RouteTileDownloadListener listener) {
    this.offlineNavigator = offlineNavigator;
    this.tilePath = tilePath;
    this.listener = listener;
  }

  void startDownload(final OfflineTiles offlineTiles) {
    String version = offlineTiles.version();
    TarFetchedCallback tarFetchedCallback = buildTarFetchedCallback(version);
    offlineTiles.fetchRouteTiles(tarFetchedCallback);
  }

  void onError(OfflineError error) {
    if (listener != null) {
      listener.onError(error);
    }
  }

  @NonNull
  private TarFetchedCallback buildTarFetchedCallback(String version) {
    DownloadTask downloadTask = buildDownloadTask(tilePath, version);
    return new TarFetchedCallback(this, downloadTask);
  }

  @NonNull
  private DownloadTask buildDownloadTask(String tilePath, String tileVersion) {
    TileUnpacker tileUnpacker = new TileUnpacker(offlineNavigator);
    DownloadUpdateListener downloadListener = new DownloadUpdateListener(
      this,
      tileUnpacker,
      tilePath,
      tileVersion,
      listener
    );
    return new DownloadTask(
      tilePath,
      tileVersion,
      FILE_EXTENSION_TAR,
      downloadListener
    );
  }
}

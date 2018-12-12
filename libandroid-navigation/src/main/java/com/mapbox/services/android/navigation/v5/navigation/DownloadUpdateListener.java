package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.utils.DownloadTask;

import java.io.File;

/**
 * Triggers a {@link TileUnpacker} to unpack the tar file into routing tiles once the FILE_EXTENSION_TAR
 * download is complete.
 */
class DownloadUpdateListener implements DownloadTask.DownloadListener {

  private static final String DOWNLOAD_ERROR_MESSAGE = "Error occurred downloading tiles: null file found";
  private final RouteTileDownloader downloader;
  private final TileUnpacker tileUnpacker;
  private final RouteTileDownloadListener listener;
  private final String destinationPath;

  DownloadUpdateListener(RouteTileDownloader downloader, TileUnpacker tileUnpacker,
                         String tilePath, String tileVersion, RouteTileDownloadListener listener) {
    this.downloader = downloader;
    this.listener = listener;
    this.tileUnpacker = tileUnpacker;
    destinationPath = buildDestinationPath(tilePath, tileVersion);
  }

  @Override
  public void onFinishedDownloading(@NonNull File file) {
    tileUnpacker.unpack(file, destinationPath, new UnpackProgressUpdateListener(listener));
  }

  @Override
  public void onErrorDownloading() {
    OfflineError error = new OfflineError(DOWNLOAD_ERROR_MESSAGE);
    downloader.onError(error);
  }

  private String buildDestinationPath(String tilePath, String tileVersion) {
    File destination = new File(tilePath, tileVersion);
    if (!destination.exists()) {
      destination.mkdirs();
    }
    return destination.getAbsolutePath();
  }
}

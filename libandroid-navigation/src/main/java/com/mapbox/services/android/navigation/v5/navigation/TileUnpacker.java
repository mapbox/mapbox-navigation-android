package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import java.io.File;

class TileUnpacker {
  private final OfflineNavigator offlineNavigator;

  TileUnpacker(OfflineNavigator offlineNavigator) {
    this.offlineNavigator = offlineNavigator;
  }

  /**
   * Unpacks a TAR file at the srcPath into the destination directory.
   *
   * @param src where TAR file is located
   * @param destPath to the destination directory
   * @param updateListener listener to listen for progress updates
   */
  void unpack(File src, String destPath, UnpackUpdateTask.ProgressUpdateListener updateListener) {
    new UnpackerTask(offlineNavigator).executeOnExecutor(
      AsyncTask.THREAD_POOL_EXECUTOR, src.getAbsolutePath(), destPath + File.separator);
    new UnpackUpdateTask(updateListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, src);
  }
}

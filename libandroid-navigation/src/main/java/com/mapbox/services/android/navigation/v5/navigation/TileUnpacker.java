package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;
import android.util.Log;

import com.mapbox.navigator.Navigator;

import java.io.File;

class TileUnpacker {
  private final Navigator navigator;

  static {
    NavigationLibraryLoader.load();
  }

  TileUnpacker() {
    this.navigator = new Navigator();
  }

  /**
   * Unpacks a TAR file at the srcPath into the destination directory.
   *
   * @param src where TAR file is located
   * @param destPath to the destination directory
   * @param updateListener listener to listen for progress updates
   */
  void unpack(File src, String destPath, UnpackUpdateTask.ProgressUpdateListener updateListener) {
    Log.d("source path", "" + src.getAbsolutePath());
    Log.d("dest path", "" + destPath);
    new UnpackerTask(navigator).executeOnExecutor(
      AsyncTask.THREAD_POOL_EXECUTOR, src.getAbsolutePath(), destPath + File.separator);
    new UnpackUpdateTask(updateListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, src);
  }
}

package com.mapbox.services.android.navigation.v5.navigation.offline;

import android.os.AsyncTask;

import java.io.File;

public class TileUnpacker {

  public void unpack(File src, String destPath, UnpackUpdateTask.UpdateListener updateListener) {
    new UnpackerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, src.getAbsolutePath(), destPath);
    new UnpackUpdateTask(updateListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, src);
  }
}

package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.File;

/**
 * This class is an {@link AsyncTask} which monitors the unpacking of a TAR file and updates a
 * listener so that the view can show the unpacking progress. It monitors the unpacking by
 * periodically checking the file size, because as it's unpacked, the file size will decrease.
 */
class UnpackUpdateTask extends AsyncTask<File, Long, File> {
  private ProgressUpdateListener progressUpdateListener;

  /**
   * Creates a new UnpackUpdateTask to update the view via a passed {@link ProgressUpdateListener}.
   *
   * @param progressUpdateListener listener to update
   */
  UnpackUpdateTask(@NonNull ProgressUpdateListener progressUpdateListener) {
    this.progressUpdateListener = progressUpdateListener;
  }

  @Override
  protected File doInBackground(File... files) {
    File tar = files[0];
    long size = tar.length();

    while (tar.length() > 0) {
      publishProgress((((tar.length() / size)) * 100));
    }

    return tar;
  }

  @Override
  protected void onPostExecute(File file) {
    super.onPostExecute(file);
    if (progressUpdateListener != null) {
      progressUpdateListener.onCompletion();
    }
  }

  @Override
  protected void onProgressUpdate(Long... values) {
    if (progressUpdateListener != null) {
      progressUpdateListener.onProgressUpdate(values[0]);
    }
  }

  /**
   * Interface to allow view to receive updates about the progress of a file unpacking.
   */
  public interface ProgressUpdateListener {
    void onProgressUpdate(Long progress);

    void onCompletion();
  }
}

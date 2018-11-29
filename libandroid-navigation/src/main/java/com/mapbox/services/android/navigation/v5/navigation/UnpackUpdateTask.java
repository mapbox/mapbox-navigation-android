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
    // As the data is unpacked from the file, the file is truncated
    // We are finished unpacking the data when the file is fully 0 bytes
    File tilePack = files[0];
    double size = tilePack.length();
    long progress = 0;
    do {
      progress = (long)(100.0 * (1.0 - (tilePack.length() / size)));
      publishProgress(progress);
    }
    while (progress < 100L);

    return tilePack;
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

package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.utils.DownloadTask;

import java.io.File;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * This class serves to contain the complicated chain of events that must happen to download
 * offline routing tiles. It creates and maintains a directory structure with the root in the
 * Offline directory, or wherever someone specifies.
 */
class RouteTileDownloader {
  private final String tilePath;
  private final RouteTileDownloadListener listener;
  private String version;
  private DownloadTask downloadTask;

  RouteTileDownloader(String tilePath, RouteTileDownloadListener listener) {
    this.tilePath = tilePath;
    this.listener = listener;
  }

  void startDownload(final OfflineTiles offlineTiles) {
    version = offlineTiles.version();
    offlineTiles.getRouteTiles(new TarFetchedCallback());
  }

  private void onError(Throwable throwable) {
    if (listener != null) {
      listener.onError(throwable);
    }
  }

  /**
   * Triggers the downloading of the TAR file included in the {@link ResponseBody} onto disk.
   */
  private class TarFetchedCallback implements Callback<ResponseBody> {

    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
      Timber.d("TAR Url: " + call.request().url());

      downloadTask = new DownloadTask(
        tilePath,
        version,
        "tar",
        new DownloadUpdateListener());
      downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, response.body());
    }

    @Override
    public void onFailure(Call<ResponseBody> call, Throwable throwable) {
      onError(throwable);
    }
  }

  /**
   * Triggers a {@link TileUnpacker} to unpack the TAR file into routing tiles once the TAR
   * download is complete.
   */
  private class DownloadUpdateListener implements DownloadTask.DownloadListener {

    @Override
    public void onFinishedDownloading(@NonNull File file) {
      File destination = new File(tilePath, version);
      if (!destination.exists()) {
        destination.mkdirs();
      }
      new TileUnpacker().unpack(file, destination.getAbsolutePath(), new UnpackProgressUpdateListener());
    }

    @Override
    public void onErrorDownloading() {
      onError(new Throwable("Error downloading"));
    }
  }

  /**
   * Updates any UI elements on the status of the TAR unpacking.
   */
  private class UnpackProgressUpdateListener implements UnpackUpdateTask.ProgressUpdateListener {

    @Override
    public void onProgressUpdate(Long progress) {
      if (listener != null) {
        listener.onProgressUpdate(progress.intValue());
      }
    }

    @Override
    public void onCompletion() {
      listener.onCompletion(true);
    }
  }
}

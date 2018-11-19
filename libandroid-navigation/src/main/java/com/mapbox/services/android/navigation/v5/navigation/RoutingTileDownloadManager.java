package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;

import com.mapbox.services.android.navigation.v5.utils.DownloadTask;

import java.io.File;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class serves to contain the complicated chain of events that must happen to download
 * offline routing tiles. It creates and maintains a directory structure with the root in the
 * Offline directory, or wherever someone specifies.
 */

public class RoutingTileDownloadManager {
  private final File tileDirectory;
  private String version;
  private RoutingTileDownloadListener listener;
  private DownloadTask.DownloadListener downloadFinishedListener;
  private DownloadTask downloadTask;

  public RoutingTileDownloadManager() {
    this(Environment.getExternalStoragePublicDirectory("Offline"));
  }

  public RoutingTileDownloadManager(File destDirectory) {
    tileDirectory = new File(destDirectory, "tiles");
  }

  public void setListener(RoutingTileDownloadListener listener) {
    this.listener = listener;
  }

  @RequiresPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
  public void startDownload(final OfflineTiles offlineTiles) {
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
      downloadFinishedListener = new DownloadUpdateListener();

      downloadTask = new DownloadTask(
        tileDirectory.getAbsolutePath(),
        version,
        "tar",
        downloadFinishedListener);
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
      String destPath = new File(tileDirectory, version).getAbsolutePath();
      new TileUnpacker().unpack(file, destPath, new UnpackUpdateListener());
    }

    @Override
    public void onErrorDownloading() {
      onError(new Throwable("Error downloading"));
    }
  }

  /**
   * Updates any UI elements on the status of the TAR unpacking.
   */
  private class UnpackUpdateListener implements UnpackUpdateTask.UpdateListener {

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

  public interface RoutingTileDownloadListener {
    void onError(Throwable throwable);

    void onProgressUpdate(int percent);

    void onCompletion(boolean successful);
  }
}

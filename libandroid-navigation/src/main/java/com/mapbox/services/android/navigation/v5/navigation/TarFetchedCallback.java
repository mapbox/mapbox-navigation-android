package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.utils.DownloadTask;

import java.util.HashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Triggers the downloading of the tar file included in the {@link ResponseBody} onto disk.
 */
class TarFetchedCallback implements Callback<ResponseBody> {

  private final RouteTileDownloader downloader;
  private final DownloadTask downloadTask;

  TarFetchedCallback(RouteTileDownloader downloader, DownloadTask downloadTask) {
    this.downloader = downloader;
    this.downloadTask = downloadTask;
  }

  @Override
  public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
    if (response.isSuccessful()) {
      downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, response.body());
    } else {
      HashMap<Integer, String> errorCodes = new HashMap<>();
      TarResponseErrorMap errorMap = new TarResponseErrorMap(errorCodes);
      OfflineError error = new OfflineError(errorMap.buildErrorMessageWith(response));
      downloader.onError(error);
    }
  }

  @Override
  public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
    OfflineError error = new OfflineError(throwable.getMessage());
    downloader.onError(error);
  }
}

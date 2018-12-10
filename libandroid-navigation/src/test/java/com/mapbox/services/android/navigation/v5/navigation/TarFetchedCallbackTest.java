package com.mapbox.services.android.navigation.v5.navigation;

import android.os.AsyncTask;

import com.mapbox.services.android.navigation.v5.utils.DownloadTask;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class TarFetchedCallbackTest {

  @Test
  public void onSuccessfulResponse_downloadTaskIsExecuted() {
    DownloadTask downloadTask = mock(DownloadTask.class);
    TarFetchedCallback callback = buildCallback(downloadTask);
    Call call = mock(Call.class);
    Response response = mock(Response.class);
    ResponseBody responseBody = mock(ResponseBody.class);
    when(response.body()).thenReturn(responseBody);
    when(response.isSuccessful()).thenReturn(true);

    callback.onResponse(call, response);

    verify(downloadTask).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, responseBody);
  }

  @Test
  public void onUnsuccessfulResponse_errorIsProvided() {
    RouteTileDownloader downloader = mock(RouteTileDownloader.class);
    TarFetchedCallback callback = buildCallback(downloader);
    Call call = mock(Call.class);
    Response response = mock(Response.class);
    when(response.isSuccessful()).thenReturn(false);

    callback.onResponse(call, response);

    verify(downloader).onError(any(OfflineError.class));
  }

  @Test
  public void onFailure_errorIsProvided() {
    RouteTileDownloader downloader = mock(RouteTileDownloader.class);
    TarFetchedCallback callback = buildCallback(downloader);
    Call call = mock(Call.class);
    Throwable throwable = mock(Throwable.class);

    callback.onFailure(call, throwable);

    verify(downloader).onError(any(OfflineError.class));
  }

  private TarFetchedCallback buildCallback(RouteTileDownloader downloader) {
    DownloadTask downloadTask = mock(DownloadTask.class);
    return new TarFetchedCallback(downloader, downloadTask);
  }

  private TarFetchedCallback buildCallback(DownloadTask downloadTask) {
    RouteTileDownloader downloader = mock(RouteTileDownloader.class);
    return new TarFetchedCallback(downloader, downloadTask);
  }
}
package com.mapbox.services.android.navigation.v5.navigation

import android.os.AsyncTask
import com.mapbox.services.android.navigation.v5.utils.DownloadTask
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.ResponseBody
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Call
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
class TarFetchedCallbackTest {

    @Test
    fun onSuccessfulResponse_downloadTaskIsExecuted() {
        val downloadTask = mockk<DownloadTask>(relaxed = true)
        val callback = buildCallback(downloadTask)
        val call = mockk<Call<ResponseBody>>()
        val response = mockk<Response<ResponseBody>>()
        val responseBody = mockk<ResponseBody>()
        every { response.body() } returns responseBody
        every { response.isSuccessful } returns true

        callback.onResponse(call, response)

        verify { downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, responseBody) }
    }

    @Test
    fun onUnsuccessfulResponse_errorIsProvided() {
        val downloader = mockk<RouteTileDownloader>(relaxed = true)
        val callback = buildCallback(downloader)
        val call = mockk<Call<ResponseBody>>()
        val response = mockk<Response<ResponseBody>>(relaxed = true)
        every { response.isSuccessful } returns false

        callback.onResponse(call, response)

        verify { downloader.onError(any()) }
    }

    @Test
    fun onFailure_errorIsProvided() {
        val downloader = mockk<RouteTileDownloader>(relaxed = true)
        val callback = buildCallback(downloader)
        val call = mockk<Call<ResponseBody>>()
        val throwable = mockk<Throwable>()
        every { throwable.message } returns "Exception"

        callback.onFailure(call, throwable)

        verify { downloader.onError(any()) }
    }

    private fun buildCallback(downloader: RouteTileDownloader): TarFetchedCallback {
        val downloadTask = mockk<DownloadTask>()
        return TarFetchedCallback(downloader, downloadTask)
    }

    private fun buildCallback(downloadTask: DownloadTask): TarFetchedCallback {
        val downloader = mockk<RouteTileDownloader>()
        return TarFetchedCallback(downloader, downloadTask)
    }
}

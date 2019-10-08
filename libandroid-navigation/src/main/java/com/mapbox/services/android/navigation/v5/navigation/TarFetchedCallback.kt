package com.mapbox.services.android.navigation.v5.navigation

import android.os.AsyncTask
import com.mapbox.services.android.navigation.v5.utils.DownloadTask
import java.util.HashMap
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Triggers the downloading of the tar file included in the [ResponseBody] onto disk.
 */
internal class TarFetchedCallback(
    private val downloader: RouteTileDownloader,
    private val downloadTask: DownloadTask
) : Callback<ResponseBody> {

    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
        if (response.isSuccessful) {
            downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, response.body())
        } else {
            // FIXME remove after kotlin migration, use default value param
            val errorCodes = HashMap<Int, String>()
            val errorMap = TarResponseErrorMap(errorCodes)
            val error = OfflineError(errorMap.buildErrorMessageWith(response))
            downloader.onError(error)
        }
    }

    override fun onFailure(call: Call<ResponseBody>, throwable: Throwable) {
        val error = OfflineError(throwable.message ?: "Tar fetching error")
        downloader.onError(error)
    }
}

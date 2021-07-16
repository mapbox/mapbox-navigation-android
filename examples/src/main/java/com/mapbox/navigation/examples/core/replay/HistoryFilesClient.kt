package com.mapbox.navigation.examples.core.replay

import android.util.Log
import androidx.annotation.Keep
import com.mapbox.navigation.core.history.MapboxHistoryReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL

data class ReplayPath(
    val title: String,
    val description: String,
    val path: String,
    val dataSource: ReplayDataSource
)

@Keep
enum class ReplayDataSource {
    HTTP_SERVER,
    ASSETS_DIRECTORY,
    FILE_DIRECTORY
}

class HistoryFilesClient {

    companion object {
        private const val TAG = "HistoryFilesClient"
        private const val BASE_URL = "https://mapbox.github.io/mapbox-navigation-history/"
        private const val INDEX_JSON_URL = BASE_URL + "index.json"
        private const val HISTORY_FILE_URL = BASE_URL + "navigation-history/"
    }

    suspend fun requestHistory(): List<ReplayPath> =
        withContext(Dispatchers.IO) {
            try {
                val result = URL(INDEX_JSON_URL).readText()
                val jsonArray = JSONArray(result)
                (0 until jsonArray.length()).map {
                    (jsonArray.get(it) as JSONObject).run {
                        ReplayPath(
                            title = getString("title"),
                            description = getString("description"),
                            path = getString("path"),
                            dataSource = ReplayDataSource.HTTP_SERVER
                        )
                    }
                }
            } catch (exception: IOException) {
                Log.e(TAG, "requestHistory onFailure: $exception")
                emptyList()
            }
        }

    suspend fun requestJsonFile(pathName: String, outputFile: File): MapboxHistoryReader? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = URL(HISTORY_FILE_URL + pathName).openStream()
                outputFile.outputStream().use { fileOut ->
                    inputStream.copyTo(fileOut)
                }
                MapboxHistoryReader(outputFile.absolutePath)
            } catch (exception: IOException) {
                Log.e(TAG, "requestJsonFile onFailure: $exception")
                null
            }
        }
}

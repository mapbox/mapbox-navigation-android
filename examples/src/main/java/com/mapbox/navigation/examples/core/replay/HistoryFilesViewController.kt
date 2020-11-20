package com.mapbox.navigation.examples.core.replay

import android.content.Context
import android.util.Log
import android.content.res.AssetManager.ACCESS_STREAMING
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.mapbox.navigation.core.replay.history.ReplayEventStream
import com.mapbox.navigation.examples.core.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.util.Collections
import java.util.zip.GZIPInputStream

class HistoryFilesViewController {

    private companion object {
        private const val TAG = "HistoryFilesViewController"
    }

    private var viewAdapter: HistoryFileAdapter? = null
    private val historyFilesApi = HistoryFilesClient()

    fun attach(
        context: Context,
        viewAdapter: HistoryFileAdapter,
        result: (ReplayEventStream?) -> Unit
    ) {
        this.viewAdapter = viewAdapter
        viewAdapter.itemClicked = { historyFileItem ->
            when (historyFileItem.dataSource) {
                ReplayDataSource.ASSETS_DIRECTORY -> requestFromAssets(
                    context.applicationContext,
                    historyFileItem,
                    result
                )
                ReplayDataSource.HISTORY_RECORDER -> requestFromFileCache(historyFileItem, result)
                ReplayDataSource.HTTP_SERVER -> requestFromServer(historyFileItem, result)
            }
        }
    }

    fun requestHistoryFiles(context: Context, connectionCallback: (Boolean) -> Unit) {
        requestHistory(context, connectionCallback)
    }

    private fun requestHistory(context: Context, connectionCallback: (Boolean) -> Unit): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            val drives = historyFilesApi.requestHistory().toMutableList()
            drives.addAll(requestHistoryDisk(context))
            drives.addAll(requestHistoryCache(context))
            connectionCallback.invoke(drives.isNotEmpty())
            viewAdapter?.data = drives.toList()
            viewAdapter?.notifyDataSetChanged()
        }
    }

    private suspend fun requestHistoryDisk(
        context: Context
    ): List<ReplayPath> = withContext(Dispatchers.IO) {
        val historyFiles: List<String> = context.assets.list("")?.toList()
            ?: Collections.emptyList()

        historyFiles.filter { it.endsWith(".json") }
            .map { fileName ->
                ReplayPath(
                    title = context.getString(R.string.history_local_history_file),
                    description = fileName,
                    path = fileName,
                    dataSource = ReplayDataSource.ASSETS_DIRECTORY
                )
            }
    }

    private suspend fun requestHistoryCache(
        context: Context
    ): List<ReplayPath> = withContext(Dispatchers.IO) {
        val cacheDirectory = context.cacheDir
        val historyDirectory = File(cacheDirectory, "history-cache")
            .also { it.mkdirs() }
        val historyFiles: List<File> = historyDirectory.listFiles()?.toList()
            ?: Collections.emptyList()

        historyFiles.map { file ->
            ReplayPath(
                title = context.getString(R.string.history_recorded_history_file),
                description = file.name,
                path = file.absolutePath,
                dataSource = ReplayDataSource.HISTORY_RECORDER
            )
        }
    }

    private fun requestFromFileCache(
        historyFileItem: ReplayPath,
        result: (ReplayEventStream?) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val data = loadFromFileCache(historyFileItem)
            val historyEventStream = ReplayEventStream(data)
            result(historyEventStream)
        }
    }

    private suspend fun loadFromFileCache(
        historyFileItem: ReplayPath
    ): JsonReader = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = File(historyFileItem.path)
                .inputStream()
            val gzipInputStream = GZIPInputStream(inputStream)
            JsonReader(InputStreamReader(gzipInputStream))
        } catch (e: IOException) {
            Log.e(TAG, "Your history file failed to open ${historyFileItem.path}: $e")
            throw e
        }
    }

    private fun requestFromServer(
        replayPath: ReplayPath,
        result: (ReplayEventStream?) -> Unit
    ): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            val replayHistoryDTO = historyFilesApi.requestJsonFile(replayPath.path)
            val historyEventStream = if (replayHistoryDTO != null) {
                val json = Gson().toJson(replayHistoryDTO)
                val jsonReader = JsonReader(StringReader(json))
                ReplayEventStream(jsonReader)
            } else {
                null
            }
            result.invoke(historyEventStream)
        }
    }

    private fun requestFromAssets(
        context: Context,
        historyFileItem: ReplayPath,
        result: (ReplayEventStream?) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val data = loadFromAssets(context, historyFileItem)
            result(data)
        }
    }

    // https://youtrack.jetbrains.com/issue/IDEA-227359
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun loadFromAssets(
        context: Context,
        historyFileItem: ReplayPath
    ): ReplayEventStream = withContext(Dispatchers.IO) {
        val inputStream: InputStream = context.assets.open(historyFileItem.path, ACCESS_STREAMING)
        val jsonReader = JsonReader(InputStreamReader(inputStream))
        ReplayEventStream(jsonReader)
    }
}

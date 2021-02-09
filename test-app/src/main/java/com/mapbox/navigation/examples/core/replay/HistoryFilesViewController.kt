package com.mapbox.navigation.examples.core.replay

import android.content.Context
import com.google.gson.Gson
import com.mapbox.navigation.core.replay.history.ReplayHistoryDTO
import com.mapbox.navigation.examples.core.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.Collections
import java.util.zip.GZIPInputStream
import kotlin.text.Charsets.UTF_8

class HistoryFilesViewController {

    private var viewAdapter: HistoryFileAdapter? = null
    private val historyFilesApi = HistoryFilesClient()

    fun attach(
        context: Context,
        viewAdapter: HistoryFileAdapter,
        result: (ReplayHistoryDTO?) -> Unit
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
        result: (ReplayHistoryDTO?) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val data = loadFromFileCache(historyFileItem)
            result(data)
        }
    }

    private suspend fun loadFromFileCache(
        historyFileItem: ReplayPath
    ): ReplayHistoryDTO? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = File(historyFileItem.path).inputStream()
            val historyData = GZIPInputStream(inputStream)
                .bufferedReader(UTF_8)
                .use { it.readText() }
            val historyDTO = Gson().fromJson(historyData, ReplayHistoryDTO::class.java)
            if (historyDTO.events.isNullOrEmpty()) {
                Timber.e("Your history file is empty ${historyFileItem.path}")
                null
            } else {
                historyDTO
            }
        } catch (e: IOException) {
            Timber.e(e, "Your history file failed to open ${historyFileItem.path}")
            throw e
        }
    }

    private fun requestFromServer(
        replayPath: ReplayPath,
        result: (ReplayHistoryDTO?) -> Unit
    ): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            val replayHistoryDTO = historyFilesApi.requestJsonFile(replayPath.path)
            result.invoke(replayHistoryDTO)
        }
    }

    private fun requestFromAssets(
        context: Context,
        historyFileItem: ReplayPath,
        result: (ReplayHistoryDTO?) -> Unit
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val data = loadFromAssets(context, historyFileItem)
            result(data)
        }
    }

    private suspend fun loadFromAssets(
        context: Context,
        historyFileItem: ReplayPath
    ): ReplayHistoryDTO? = withContext(Dispatchers.IO) {
        // This stores the whole file in memory and causes OutOfMemoryExceptions if the file
        // is too large. Larger project move the file into something like a Room database
        // and then read it from there.
        val historyData = try {
            val inputStream: InputStream = context.assets.open(historyFileItem.path)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (e: IOException) {
            Timber.e(e, "Your history file failed to open ${historyFileItem.path}")
            throw e
        }
        Gson().fromJson(historyData, ReplayHistoryDTO::class.java)
    }
}

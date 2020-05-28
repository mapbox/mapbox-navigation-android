package com.mapbox.navigation.examples.history

import android.content.Context
import com.google.gson.Gson
import com.mapbox.base.common.logger.model.Message
import com.mapbox.common.logger.MapboxLogger
import com.mapbox.navigation.core.replay.history.ReplayHistoryDTO
import com.mapbox.navigation.examples.R
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryFilesViewController {

    private var viewAdapter: HistoryFileAdapter? = null
    private val historyFilesApi = HistoryFilesClient()

    fun attach(context: Context, viewAdapter: HistoryFileAdapter, result: (ReplayHistoryDTO?) -> Unit) {
        this.viewAdapter = viewAdapter
        viewAdapter.itemClicked = { historyFileItem ->
            if (historyFileItem.isOnDisk()) {
                requestFromDisk(context.applicationContext, historyFileItem, result)
            } else {
                requestHistoryData(historyFileItem, result)
            }
        }
    }

    fun requestHistoryFiles(context: Context, connectionCallback: (Boolean) -> Unit) {
        requestHistory(context, connectionCallback)
    }

    private fun requestHistoryData(replayPath: ReplayPath, result: (ReplayHistoryDTO?) -> Unit): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            val replayHistoryDTO = historyFilesApi.requestJsonFile(replayPath.path)
            result.invoke(replayHistoryDTO)
        }
    }

    private fun requestHistory(context: Context, connectionCallback: (Boolean) -> Unit): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            val drives = historyFilesApi.requestHistory().toMutableList()
            drives.addAll(requestHistoryDisk(context))
            connectionCallback.invoke(drives.isNotEmpty())
            viewAdapter?.data = drives.toList()
            viewAdapter?.notifyDataSetChanged()
        }
    }

    private fun requestFromDisk(context: Context, historyFileItem: ReplayPath, result: (ReplayHistoryDTO?) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            val data = loadFromDisk(context, historyFileItem)
            result(data)
        }
    }

    private suspend fun requestHistoryDisk(context: Context): List<ReplayPath> = withContext(Dispatchers.IO) {
        val historyFiles: List<String> = context.assets.list("")?.toList()
            ?: Collections.emptyList()

        historyFiles.filter { it.endsWith(".json") }
            .map { fileName ->
                ReplayPath(
                    context.getString(R.string.history_local_history_file),
                    fileName,
                    "$LOCAL_FILE_PREFIX$fileName"
                )
            }
    }

    private suspend fun loadFromDisk(context: Context, historyFileItem: ReplayPath): ReplayHistoryDTO? = withContext(Dispatchers.IO) {
        val fileName = historyFileItem.path.removePrefix(LOCAL_FILE_PREFIX)
        val historyData = loadHistoryJsonFromAssets(context, fileName)
        Gson().fromJson(historyData, ReplayHistoryDTO::class.java)
    }

    private suspend fun loadHistoryJsonFromAssets(context: Context, fileName: String): String = withContext(Dispatchers.IO) {
        // This stores the whole file in memory and causes OutOfMemoryExceptions if the file
        // is too large. Larger project move the file into something like a Room database
        // and then read it from there.
        try {
            val inputStream: InputStream = context.assets.open(fileName)
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.forName("UTF-8"))
        } catch (e: IOException) {
            MapboxLogger.e(
                Message("Your history file failed to open $fileName"),
                e
            )
            throw e
        }
    }

    companion object {
        private const val LOCAL_FILE_PREFIX = "assets/"

        private fun ReplayPath.isOnDisk(): Boolean {
            return path.startsWith(LOCAL_FILE_PREFIX)
        }
    }
}

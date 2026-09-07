package com.mapbox.navigation.testing.utils.history

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.testing.ui.utils.coroutines.stopRecording
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

/**
 * Add this TestRule to your test and the directory will be saved
 * to external storage. This allows you to then pull the file and
 * debug potential issues in instrumentation tests.
 *
 * Adding rule to your test, example:
 * @get:Rule
 * val historyTestRule = MapboxHistoryTestRule()
 *
 * @Before
 * fun setup() {
 *     mapboxNavigation = MapboxNavigationProvider.create(..)
 *     historyTestRule.historyRecorder = mapboxNavigation.historyRecorder
 * }
 *
 * Download files, example:
 * View the results on the device
 *   adb shell "cd sdcard/Download/mapbox_test && ls"
 * Pull the results onto your desktop
 *   adb pull sdcard/Download/mapbox_test my-local-folder
 */
class MapboxHistoryTestRule : TestWatcher() {

    private val mapboxTestDirectoryName = "mapbox_test"
    private val directory: File =
        File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ),
            mapboxTestDirectoryName
        )

    lateinit var historyRecorder: MapboxHistoryRecorder
    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    override fun finished(description: Description) {
        if (::historyRecorder.isInitialized) {
            val filePath = historyRecorder.fileDirectory()!!
            val file = File(filePath)
            file.walk().filterNot { it.isDirectory }.forEach {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveFileUsingMediaStore(it, context, description.methodName, it.name)
                } else {
                    val path = description.methodName + File.separator + it.name
                    val target = File(directory, path)
                    it.copyTo(target)
                }
            }
        }
    }

    fun stopRecordingOnCrash(message: String, runner: () -> Unit) {
        try {
            runner()
        } catch (t: Throwable) {
            runBlocking(Dispatchers.Main) {
                val path = historyRecorder.stopRecording()
                logE("$message history path=$path", "DEBUG")
            }
            throw t
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveFileUsingMediaStore(
        inputFile: File?,
        context: Context,
        filePath: String,
        fileName: String
    ) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/$mapboxTestDirectoryName/" + filePath
            )
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            inputFile?.inputStream().use { input ->
                resolver.openOutputStream(uri).use { output ->
                    input?.copyTo(output!!, DEFAULT_BUFFER_SIZE)
                }
            }
        }
    }
}

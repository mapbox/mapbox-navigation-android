package com.mapbox.navigation.instrumentation_tests.utils.history

import android.os.Environment
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
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
 *     mapboxNavigation = MapboxNavigation(..)
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

    private val directory: File =
        File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ),
            "mapbox_test"
        )

    lateinit var historyRecorder: MapboxHistoryRecorder

    override fun finished(description: Description) {
        val filePath = historyRecorder.fileDirectory()!!
        val file = File(filePath)
        file.walk().filterNot { it.isDirectory }.forEach {
            val path = description.methodName + File.separator + it.name
            val target = File(directory, path)
            it.copyTo(target)
        }
    }
}

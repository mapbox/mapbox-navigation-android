package com.mapbox.services.android.navigation.v5.navigation

import android.os.AsyncTask
import java.io.File

/**
 * Takes in a string for a path to a TAR file containing routing tiles, and unpacks them to the
 * specified destination path. The path to the TAR file containing routing tiles and the path to
 * the directory in which to unpack the tiles are included in the params passed to this AsyncTask.
 * The first string should be the path to the TAR file, and the second string should be the path
 * to the destination directory for the resulting tiles.
 */
internal class UnpackerTask(private val offlineNavigator: OfflineNavigator) :
    AsyncTask<String, Int, File>() {

    companion object {
        private const val TAR_PATH_POSITION = 0
        private const val DESTINATION_PATH_POSITION = 1
    }

    override fun doInBackground(vararg strings: String): File {
        offlineNavigator.unpackTiles(strings[TAR_PATH_POSITION], strings[DESTINATION_PATH_POSITION])

        return File(strings[0])
    }

    override fun onPostExecute(file: File) {
        file.delete()
    }
}

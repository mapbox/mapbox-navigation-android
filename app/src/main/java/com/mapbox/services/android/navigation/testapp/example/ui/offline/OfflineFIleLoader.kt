package com.mapbox.services.android.navigation.testapp.example.ui.offline

import android.os.Environment
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import timber.log.Timber
import java.io.File

class OfflineFileLoader(private val navigation: MapboxNavigation,
                        private val callback: OfflineFilesLoadedCallback) {
  init {
    doAsync {
      val tilesDirPath = getOfflineDirectoryFile("tiles")
      val translationsDirPath = getOfflineDirectoryFile("translations")
      navigation.initializeOfflineData(tilesDirPath, translationsDirPath)
      uiThread { 
        callback.onFilesLoaded()
      }
    }
  }

  private fun getOfflineDirectoryFile(fileName: String): String {
    val offline = Environment.getExternalStoragePublicDirectory("Offline")
    if (!offline.exists()) {
      Timber.d("Offline directory does not exist")
    }
    val file = File(offline, fileName)
    return file.absolutePath
  }
}
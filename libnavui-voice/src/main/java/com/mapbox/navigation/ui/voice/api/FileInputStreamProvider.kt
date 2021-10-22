package com.mapbox.navigation.ui.voice.api

import java.io.File
import java.io.FileInputStream

internal object FileInputStreamProvider {

    fun retrieveFileInputStream(file: File): FileInputStream {
        return FileInputStream(file)
    }
}

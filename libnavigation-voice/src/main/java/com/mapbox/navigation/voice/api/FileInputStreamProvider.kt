package com.mapbox.navigation.voice.api

import java.io.File
import java.io.FileInputStream

internal object FileInputStreamProvider {

    fun retrieveFileInputStream(file: File): FileInputStream {
        return FileInputStream(file)
    }
}

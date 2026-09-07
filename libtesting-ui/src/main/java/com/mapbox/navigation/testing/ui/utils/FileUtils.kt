package com.mapbox.navigation.testing.ui.utils

import com.adevinta.android.barista.rule.cleardata.internal.FileOperations

fun clearInternalStorage() {
    val fileOperations = FileOperations()
    fileOperations.getAllFilesRecursively()
        .forEach { fileOperations.deleteFile(it) }
}

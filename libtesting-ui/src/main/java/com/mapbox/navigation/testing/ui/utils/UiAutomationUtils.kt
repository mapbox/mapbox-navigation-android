package com.mapbox.navigation.testing.ui.utils

import android.app.UiAutomation
import java.io.FileInputStream

fun UiAutomation.executeShellCommandBlocking(command: String): ByteArray {
    val output = executeShellCommand(command)
    return FileInputStream(output.fileDescriptor).use { it.readBytes() }
}

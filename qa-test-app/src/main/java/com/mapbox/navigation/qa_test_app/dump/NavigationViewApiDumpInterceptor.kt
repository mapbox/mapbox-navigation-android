package com.mapbox.navigation.qa_test_app.dump

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.dump.MapboxDumpInterceptor
import com.mapbox.navigation.dropin.NavigationViewApi
import java.io.FileDescriptor
import java.io.PrintWriter

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class NavigationViewApiDumpInterceptor(
    private val navigationViewApi: NavigationViewApi
) : MapboxDumpInterceptor {

    override fun command(): String = COMMAND

    override fun description(): String = "Allows you to update navigation view state"

    override fun availableCommands(): List<Pair<String, String>> = availableCommands

    override fun intercept(
        fileDescriptor: FileDescriptor,
        writer: PrintWriter,
        commands: List<String>
    ) {
        commands.forEach { command ->
            when (command) {
                COMMAND_START_FREE_DRIVE -> navigationViewApi.startFreeDrive()
            }
        }
    }

    private companion object {
        private const val COMMAND = "dropin"
        private const val COMMAND_START_FREE_DRIVE = "$COMMAND:start_free_drive"

        private val availableCommands = listOf(
            COMMAND_START_FREE_DRIVE to "Change the trip session state to free drive"
        )
    }
}

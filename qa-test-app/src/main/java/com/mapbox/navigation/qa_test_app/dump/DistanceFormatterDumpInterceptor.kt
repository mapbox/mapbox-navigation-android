package com.mapbox.navigation.qa_test_app.dump

import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.core.internal.dump.MapboxDumpInterceptor
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import java.io.FileDescriptor
import java.io.PrintWriter

class DistanceFormatterDumpInterceptor : MapboxDumpInterceptor {
    override fun command() = COMMAND

    override fun description(): String = "Allows you to change the distance formatting options"

    override fun availableCommands(): List<Pair<String, String>> = availableCommands

    override fun intercept(
        fileDescriptor: FileDescriptor,
        writer: PrintWriter,
        commands: List<String>
    ) {
        commands.forEach { command ->
            when (command) {
                COMMAND_UNIT_TYPE -> updateUnitType(writer, null)
                COMMAND_UNIT_TYPE_METRIC -> updateUnitType(writer, UnitType.METRIC)
                COMMAND_UNIT_TYPE_IMPERIAL -> updateUnitType(writer, UnitType.IMPERIAL)
            }
        }
    }

    private fun updateUnitType(writer: PrintWriter, unitType: UnitType?) {
        MapboxNavigationApp.current()?.navigationOptions?.let { currentOptions ->
            if (currentOptions.distanceFormatterOptions.unitType != unitType) {
                MapboxNavigationApp.disable()
                MapboxNavigationApp.setup(
                    currentOptions.toBuilder().distanceFormatterOptions(
                        currentOptions.distanceFormatterOptions.toBuilder()
                            .unitType(unitType)
                            .build()
                    ).build()
                )
                val currentUnitType = currentOptions.distanceFormatterOptions.unitType
                writer.println("Updated unit type changed from $currentUnitType to $unitType")
            }
        }
    }

    private companion object {
        private const val COMMAND = "distance_formatter"
        private const val COMMAND_UNIT_TYPE = "$COMMAND:unit_type"
        private const val COMMAND_UNIT_TYPE_METRIC = "$COMMAND_UNIT_TYPE:metric"
        private const val COMMAND_UNIT_TYPE_IMPERIAL = "$COMMAND_UNIT_TYPE:imperial"

        private val availableCommands = listOf(
            Pair(COMMAND_UNIT_TYPE, "Use the device local unit type"),
            Pair(COMMAND_UNIT_TYPE_METRIC, "Change to unit type to metric"),
            Pair(COMMAND_UNIT_TYPE_IMPERIAL, "Change to unit type to imperial"),
        )
    }
}

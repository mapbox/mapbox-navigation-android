package com.mapbox.navigation.core.internal.dump

import java.io.FileDescriptor
import java.io.PrintWriter

/**
 * This will process commands from the dumpsys. Use the [MapboxDumpRegistry] to control what will
 * happen when the dump command is called.
 *
 * @see MapboxDumpRegistry registry for interacting with the interceptors
 */
internal class MapboxDumpHandler {

    fun handle(fd: FileDescriptor, writer: PrintWriter, args: Array<String>?) {
        val handled = handleArguments(fd, writer, args)
        if (handled.isEmpty()) {
            // Call the default command when no interceptors are recognized.
            MapboxDumpRegistry.defaultInterceptor?.intercept(fd, writer, emptyList())
        }
    }

    private fun handleArguments(
        fd: FileDescriptor,
        writer: PrintWriter,
        args: Array<String>?,
    ): List<MapboxDumpInterceptor> {
        val matches: List<Pair<List<MapboxDumpInterceptor>, List<String>>> = args
            ?.groupBy { it.substringBefore(":", it) }
            ?.map { Pair(MapboxDumpRegistry.getInterceptors(it.key), it.value) }
            ?: return emptyList()

        matches.forEach { match ->
            val interceptors = match.first
            val commands = match.second
            if (interceptors.isEmpty()) {
                writer.println("Unrecognized commands: ${commands.joinToString()}")
            } else {
                interceptors.forEach { it.intercept(fd, writer, commands) }
                writer.println("Processed: ${commands.joinToString()}")
            }
        }
        return matches.flatMap { it.first }
    }
}

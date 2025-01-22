package com.mapbox.navigation.core.internal.dump

import android.app.Service
import com.google.gson.JsonSyntaxException
import java.io.FileDescriptor
import java.io.PrintWriter

/**
 * This is an interface that allows you to intercept command line arguments from dumpsys.
 *
 * All arguments beginning with `your_command:*` will be sent to the [intercept] function.
 *
 * For example, if you have an interceptor called "nav_feature".
 *   service=com.mapbox.navigation.core.trip.service.NavigationNotificationService
 *   $ adb shell dumpsys activity service ${service} nav_feature:one nav_feature:two
 * The intercept function will receive listOf("nav_feature:one", "nav_feature:two")
 */
interface MapboxDumpInterceptor {
    /**
     * String representing the type of commands this interceptor can handle.
     */
    fun command(): String

    /**
     * Human readable description of what this interceptor is going to do when the command is
     * passed.
     *
     * All available commands can be viewed with the `help` command
     *   $ adb shell dumpsys activity service {service-path} help
     */
    fun description(): String

    /**
     * Human readable list of each command and a description of what the command will do. This will
     * be displayed with the `help:$command`
     *
     * @return pair of strings where the first is the `command` and the second is a `description`.
     */
    fun availableCommands(): List<Pair<String, String>>

    /**
     * When a command is found by [Service.dump], the result will be passed to this interceptor.
     *
     * @throws JsonSyntaxException the caller is expected to handle json exceptions.
     */
    @Throws(JsonSyntaxException::class)
    fun intercept(
        fileDescriptor: FileDescriptor,
        writer: PrintWriter,
        commands: List<String>,
    )
}

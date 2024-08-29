package com.mapbox.navigation.core.internal.dump

import java.io.FileDescriptor
import java.io.PrintWriter

/**
 * Default interceptor which provides help for the adb service interface.
 */
class HelpDumpInterceptor : MapboxDumpInterceptor {
    override fun command() = "help"

    override fun description(): String = "Shows available commands and instructions"

    override fun availableCommands(): List<Pair<String, String>> {
        return MapboxDumpRegistry.getInterceptors().filter { it != this }.map {
            Pair(
                "${command()}:${it.command()}",
                "Get the commands available from ${it.command()}",
            )
        }
    }

    override fun intercept(
        fileDescriptor: FileDescriptor,
        writer: PrintWriter,
        commands: List<String>,
    ) {
        if (commands.isEmpty()) {
            printHelpFullDescription(writer)
        } else if (commands.size == 1 && commands[0] == command()) {
            printHelpCommandList(writer)
        } else {
            commands.forEach { command ->
                val interceptorCommand = command.substringAfter("${command()}:")
                val interceptors = MapboxDumpRegistry.getInterceptors(interceptorCommand)
                if (interceptors.isEmpty()) {
                    writer.println("Could not find $command")
                } else {
                    writer.println("Available commands for $command")
                    interceptors.forEach { interceptor ->
                        interceptor.availableCommands().forEach {
                            writer.println("   ${it.prettyString()}")
                        }
                    }
                }
            }
        }
    }

    private fun List<Pair<String, String>>.prettyString() = joinToString(
        separator = System.lineSeparator(),
        transform = { it.prettyString() },
    )

    private fun Pair<String, String>.prettyString() = "$first, $second"

    private fun printHelpFullDescription(writer: PrintWriter) {
        writer.println(
            """
Hello and welcome to the Mapbox Navigation dump! 
  This allows you to control Mapbox Navigation
  from adb. Below are the commands and shortcuts
  that are available. If you'd like to create your
  own commands, look at the `MapboxDumpRegistry`.

Command arguments can be passed as key:value and are separated by spaces.
  For example, if you pass data to dumpsys
  and you have added a `MapboxDumpInterceptor`, your
  interceptor will receive the command and the data.
  
  $ adb shell dumpsys activity service <service-package> turn_off_audio_guidance
  >> turn_off_audio_guidance
  
  $ adb shell dumpsys activity service <service-package> months:june months:july
  >> months:june months:july

  $ adb shell dumpsys activity service <service-package> "animal":{"age":4,"name":"cat","weight":{"units":"kilograms","value":4.5}}
  >> args[0] = animal:age:4
  >> args[1] = animal:name:cat
  >> args[2] = animal:weight:units:kilograms
  >> args[3] = animal:weight:value:4.5
  
  Warning: json format may give unexpected results because arguments are split by spaces.
  $ adb shell dumpsys activity service <service-package> "name":"big cat"
  >> args[0] = name:big
  >> args[1] = cat

Request help for the commands available. This list is given with the `help` command.
${availableCommands().prettyString()}
            """.trimIndent(),
        )
    }

    private fun printHelpCommandList(writer: PrintWriter) {
        writer.println("Request help for the commands available")
        writer.println(availableCommands().prettyString())
    }
}

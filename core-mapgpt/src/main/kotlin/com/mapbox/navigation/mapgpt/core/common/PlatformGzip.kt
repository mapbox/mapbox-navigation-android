package com.mapbox.navigation.mapgpt.core.common

import io.ktor.utils.io.core.toByteArray
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

private const val TAG = "PlatformGzip"

@OptIn(ExperimentalTime::class)
fun sharedGzipCompress(input: String): ByteArray? {
    val startTimeMark = TimeSource.Monotonic.markNow()
    return PlatformGzip.compress(input)?.also { compressedBytes ->
        logResult(input, compressedBytes, startTimeMark)
    }
}

object PlatformGzip {

    fun compress(input: String): ByteArray? {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(Charsets.UTF_8).use { it.write(input) }
        return bos.toByteArray()
    }

    fun decompress(input: ByteArray): String? {
        val gzipInputStream = GZIPInputStream(input.inputStream())
        return gzipInputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}


@OptIn(ExperimentalTime::class)
private fun logResult(
    input: String,
    compressedBytes: ByteArray,
    startTimeMark: TimeSource.Monotonic.ValueTimeMark,
) {
    val originalBytes = input.toByteArray()
    val readyTimeMark = TimeSource.Monotonic.markNow()
    SharedLog.d(TAG) {
        val compressTime = readyTimeMark.minus(startTimeMark)
            .toDouble(DurationUnit.MILLISECONDS)
        val improvedBytes = (input.toByteArray().size - compressedBytes.size).toDouble()
        val improvedPercentage = (improvedBytes / originalBytes.size) * 100
        """
            Uncompressed request size: ${originalBytes.size} bytes
            Compressed request size: ${compressedBytes.size} bytes
            Improvement: ${improvedPercentage.toInt()}%
            Compressing took: ${compressTime}ms
        """.trimIndent()
    }
}

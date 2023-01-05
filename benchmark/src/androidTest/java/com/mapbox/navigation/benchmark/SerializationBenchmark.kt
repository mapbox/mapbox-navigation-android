package com.mapbox.navigation.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.api.directions.v5.models.DirectionsResponse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Benchmark, which will execute on an Android device.
 *
 * The body of [BenchmarkRule.measureRepeated] is measured in a loop, and Studio will
 * output the result. Modify your code to see how it affects performance.
 */
@RunWith(AndroidJUnit4::class)
class SerializationBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    val context get() = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun serialize_from_string() {
        val json = readDirectionsResponseFromResources()
        benchmarkRule.measureRepeated {
            DirectionsResponse.fromJson(json)
        }
    }

    @Test
    fun serialize_from_bytebuffer() {
        val json = readDirectionsResponseFromResources()
        val charset = StandardCharsets.UTF_8
        val buffer = putTextToDirectByteBuffer(json, StandardCharsets.UTF_8)
        benchmarkRule.measureRepeated {
            this.runWithTimingDisabled { buffer.position(0) }
            DirectionsResponse.fromJson(buffer, charset)
        }
    }

    private fun readDirectionsResponseFromResources(): String {
        return context.resources.openRawResource(R.raw.long_routes).bufferedReader().use { it.readText() }
    }

    private fun putTextToDirectByteBuffer(textJson: String, charset: Charset): ByteBuffer {
        val json = textJson.toByteArray(charset)
        val buffer = ByteBuffer.allocateDirect(json.size)
        buffer.put(json)
        buffer.position(0)
        return buffer
    }

}
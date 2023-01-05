package com.mapbox.navigation.instrumentation_tests.core

import android.app.Application
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.api.directions.v5.models.DirectionsResponse
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import org.junit.Before
import org.junit.Rule

class MemoryConsumptionTest {

    @Test
    fun serialize_from_string() {
        val json = readTestResponseFromResources()
        System.gc()
        Thread.sleep(20_000) // manually connect profiler and start allocation recording
        DirectionsResponse.fromJson(json)
    }

    @Test
    fun serialize_from_bytebuffer() {
        val json = readTestResponseFromResources()
        val charset = StandardCharsets.UTF_8
        val buffer = putTextToDirectByteBuffer(json, StandardCharsets.UTF_8)
        System.gc()
        Thread.sleep(20_000) // manually connect profiler and start allocation recording
        DirectionsResponse.fromJson(buffer, charset)
    }

    private fun readTestResponseFromResources(): String {
        val context = ApplicationProvider.getApplicationContext<Application>()
        return context.resources.openRawResource(R.raw.long_routes)
            .bufferedReader()
            .use { it.readText() }
    }

    private fun putTextToDirectByteBuffer(textJson: String, charset: Charset): ByteBuffer {
        val json = textJson.toByteArray(charset)

        val buffer = ByteBuffer.allocateDirect(json.size)
        buffer.compact()
        buffer.put(json)
        buffer.position(0)
        return buffer
    }
}
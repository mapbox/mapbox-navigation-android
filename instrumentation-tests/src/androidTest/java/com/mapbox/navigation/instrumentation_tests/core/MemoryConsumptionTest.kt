package com.mapbox.navigation.instrumentation_tests.core

import android.app.Application
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.bindgen.DataRef
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.testing.ui.BaseTest
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import java.lang.ref.WeakReference

class MemoryConsumptionTest {

    @Test
    fun serialize_from_string() {
        val json = readTestResponseFromResources()
        Thread.sleep(20_000) // manually connect profiler and start allocation recording
        DirectionsResponse.fromJson(json)
    }

    @Test
    fun serialize_from_bytebuffer() {
        val charset = StandardCharsets.UTF_8
        val dataRef = putTextToDirectByteBuffer(readTestResponseFromResources(), StandardCharsets.UTF_8)
        Thread.sleep(20_000) // manually connect profiler and start allocation recording
        DirectionsResponse.fromJson(dataRef.buffer, charset)
    }

    private fun readTestResponseFromResources(): String {
        val context = ApplicationProvider.getApplicationContext<Application>()
        return context.resources.openRawResource(R.raw.long_routes)
            .bufferedReader()
            .use { it.readText() }
    }

    private fun putTextToDirectByteBuffer(textJson: String, charset: Charset): DataRef {
        val json = textJson.toByteArray(charset)
        val dataRef = DataRef.allocateNative(json.size)
        dataRef.buffer.apply {
            put(json)
            position(0)
        }
        return dataRef
    }
}
package com.mapbox.navigation.voicefeedback

import android.content.Context
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.MapboxNavigationRule
import com.mapbox.navigation.testing.createMapboxNavigation
import com.mapbox.navigation.testing.voicefeedback.InputStreamMicrophone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Use these tests as a faster alternative to perform e2e test instead of integrating changes into
 * a test application.
 */
@LargeTest
@Ignore("These tests hit a real server and are not designed to be continuously run in CI.")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalTime::class)
class FeedbackAgentE2ETest {
    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()

    val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    lateinit var options: FeedbackAgentOptions

    @Before
    fun setup() {
        options = FeedbackAgentOptions.Builder()
            .language(Locale.getDefault())
            .endpoint(FeedbackAgentEndpoint.Testing)
            .microphone(
                InputStreamMicrophone {
                    context.assets.open("test_report.wav")
                },
            ).build()
    }

    @Test
    fun testFeedbackReport() = runBlocking(Dispatchers.Main.immediate) {
        withTimeout(30.seconds) {
            val mapboxNavigation =
                createMapboxNavigation(context)
            val feedbackAgent =
                FeedbackAgentSession.Builder().options(options).build()
            feedbackAgent.onAttached(mapboxNavigation)
            feedbackAgent.connect()

            // wait for the session to be connected
            feedbackAgent.asrState.first { it is ASRState.Idle }

            val transcription = async {
                feedbackAgent.asrState
                    .dropWhile { it !is ASRState.Listening }
                    .takeWhile { it is ASRState.Listening }
                    .filterIsInstance<ASRState.Listening>()
                    .map { it.text }
                    .lastOrNull()
            }
            feedbackAgent.startListening()

            // wait for the audio input to be processed
            feedbackAgent.asrState
                .filterIsInstance<ASRState.Result>()
                .firstOrNull()

            assertEquals(
                "I'm sending a test report there is a car accident right here",
                transcription.await(),
            )
        }
    }
}

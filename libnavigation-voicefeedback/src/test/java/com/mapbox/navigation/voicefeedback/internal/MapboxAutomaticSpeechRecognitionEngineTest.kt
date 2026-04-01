package com.mapbox.navigation.voicefeedback.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.voicefeedback.FakeInputStreamMicrophone
import com.mapbox.navigation.voicefeedback.ASRState
import com.mapbox.navigation.voicefeedback.Microphone
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@OptIn(
    ExperimentalPreviewMapboxNavigationAPI::class,
    ExperimentalCoroutinesApi::class,
    ExperimentalTime::class,
)
class MapboxAutomaticSpeechRecognitionEngineTest {

    @get:Rule
    val logRule = LoggingFrontendTestRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private lateinit var testScope: TestCoroutineScope
    private lateinit var backgroundScope: TestCoroutineScope
    private val sessionStateFlow = MutableStateFlow<AsrSessionState>(AsrSessionState.Disconnected)
    private val asrDataFlow = MutableSharedFlow<AsrData?>()
    private val mapboxASRService = mockk<MapboxASRService>(relaxed = true) {
        every { sessionState } returns sessionStateFlow
        every { asrData } returns asrDataFlow
    }

    private fun createMicrophone(): Microphone = FakeInputStreamMicrophone {
        javaClass.classLoader?.getResourceAsStream("test_report.wav")
            ?: error("test_report.wav not found in test resources")
    }

    private fun createEngine(
        scope: CoroutineScope,
        stoppedSpeakingThreshold: Duration = 6.toDuration(DurationUnit.SECONDS),
        resultTimeout: Duration = 5.toDuration(DurationUnit.SECONDS),
        checkSpeakingInterval: Duration = 1.toDuration(DurationUnit.SECONDS),
    ): MapboxAutomaticSpeechRecognitionEngine = MapboxAutomaticSpeechRecognitionEngine(
        mapboxASRService = mapboxASRService,
        microphone = createMicrophone(),
        scope = scope,
        stoppedSpeakingThreshold = stoppedSpeakingThreshold,
        resultTimeout = resultTimeout,
        checkSpeakingInterval = checkSpeakingInterval,
    )

    @Before
    fun setUp() {
        testScope = TestCoroutineScope(testDispatcher)
        backgroundScope = TestCoroutineScope()
        Dispatchers.setMain(testDispatcher)
        sessionStateFlow.value = AsrSessionState.Disconnected
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        backgroundScope.cleanupTestCoroutines()
        testScope.cleanupTestCoroutines()
    }

    @Test
    fun `when service is connected then engine is idle`() = testScope.runBlockingTest {
        val engine = createEngine(backgroundScope)
        runCurrent()

        sessionStateFlow.value = AsrSessionState.Connected("https://asr.example.com", "session-1")
        runCurrent()

        val state = engine.state.first { it != null }
        Assert.assertTrue(state is ASRState.Idle)
    }

    @Test
    fun `when service is connecting then engine state is null`() = testScope.runBlockingTest {
        val engine = createEngine(backgroundScope)
        runCurrent()

        sessionStateFlow.value = AsrSessionState.Connecting("https://asr.example.com", "sid")
        runCurrent()

        Assert.assertNull(engine.state.value)
    }

    @Test
    fun `when service is disconnected then engine state is null`() = testScope.runBlockingTest {
        val engine = createEngine(backgroundScope)
        runCurrent()

        sessionStateFlow.value = AsrSessionState.Disconnected
        runCurrent()

        Assert.assertNull(engine.state.value)
    }

    @Test
    fun `when startListening invoked then engine emits Listening`() = testScope.runBlockingTest {
        sessionStateFlow.value = AsrSessionState.Connected("https://asr.example.com", "session-1")
        val engine = createEngine(backgroundScope)
        runCurrent()

        engine.startListening()
        runCurrent()

        val state = engine.state.value
        Assert.assertTrue(state is ASRState.Listening)
        Assert.assertEquals("", (state as ASRState.Listening).text)
    }

    @Test
    fun `when transcript received then engine emits text`() = testScope.runBlockingTest {
        sessionStateFlow.value = AsrSessionState.Connected("https://asr.example.com", "session-1")
        val engine = createEngine(backgroundScope)
        runCurrent()

        engine.startListening()
        runCurrent()

        launch {
            asrDataFlow.emit(AsrData.Transcript("hello world", isFinal = false))
        }
        runCurrent()

        val state = engine.state.value
        Assert.assertTrue(state is ASRState.Listening)
        Assert.assertEquals("hello world", (state as ASRState.Listening).text)
    }

    @Test
    fun `when final transcript received then engine emits SpeechFinishedWaitingForResult`() =
        testScope.runBlockingTest {
            sessionStateFlow.value =
                AsrSessionState.Connected("https://asr.example.com", "session-1")
            val engine = createEngine(backgroundScope)
            runCurrent()

            engine.startListening()
            runCurrent()

            launch {
                asrDataFlow.emit(AsrData.Transcript("final transcription", isFinal = true))
            }
            runCurrent()

            val state = engine.state.value
            Assert.assertTrue(state is ASRState.SpeechFinishedWaitingForResult)
        }

    @Test
    fun `when ASR Result received then engine emits Result`() = testScope.runBlockingTest {
        sessionStateFlow.value = AsrSessionState.Connected("https://asr.example.com", "session-1")
        val engine = createEngine(backgroundScope)
        runCurrent()

        engine.startListening()
        runCurrent()

        launch {
            asrDataFlow.emit(AsrData.Result("Bug report description", "bug_report"))
        }
        runCurrent()

        val state = engine.state.value
        Assert.assertTrue(state is ASRState.Result)
        val resultState = state as ASRState.Result
        Assert.assertEquals("Bug report description", resultState.text)
        Assert.assertEquals("bug_report", resultState.feedbackType)
    }

    @Test
    fun `when microphone Error occurs then engine emits Error state`() = testScope.runBlockingTest {
        val errorMicrophone = mockk<Microphone>(relaxed = true) {
            every { config } returns Microphone.Config()
            every { state } returns MutableStateFlow(Microphone.State.Error("permission denied"))
        }

        sessionStateFlow.value = AsrSessionState.Connected("https://asr.example.com", "session-1")
        val engine = MapboxAutomaticSpeechRecognitionEngine(
            mapboxASRService = mapboxASRService,
            microphone = errorMicrophone,
            scope = backgroundScope,
        )
        runCurrent()

        val state = engine.state.value
        Assert.assertTrue(state is ASRState.Error)
        Assert.assertNotNull((state as ASRState.Error).error.message)
        Assert.assertTrue((state).error.message!!.contains("permission denied"))
    }

    @Test
    fun `when stopListening invoked then engine emits Idle`() = testScope.runBlockingTest {
        sessionStateFlow.value = AsrSessionState.Connected("https://asr.example.com", "session-1")
        val engine = createEngine(backgroundScope)
        runCurrent()

        engine.startListening()
        runCurrent()

        engine.stopListening()
        runCurrent()

        val state = engine.state.value
        Assert.assertTrue(state is ASRState.Idle)
    }

    @Test
    fun `when stopListening invoked then engine stops microphone`() = testScope.runBlockingTest {
        sessionStateFlow.value = AsrSessionState.Connected("https://asr.example.com", "session-1")
        val engine = createEngine(backgroundScope)
        runCurrent()

        engine.startListening()
        runCurrent()

        engine.stopListening()
        runCurrent()

        verify { mapboxASRService.sendFinalAsrData(false) }
    }

    @Test
    fun `when interruptListening invoked then engine emits Interrupted`() = testScope.runBlockingTest {
        sessionStateFlow.value = AsrSessionState.Connected("https://asr.example.com", "session-1")
        val engine = createEngine(backgroundScope)
        runCurrent()

        engine.startListening()
        runCurrent()

        engine.interruptListening()
        runCurrent()

        val state = engine.state.value
        Assert.assertTrue(state is ASRState.Interrupted)
    }

    @Test
    fun `when interruptListening invoked then engine sends abort`() = testScope.runBlockingTest {
        sessionStateFlow.value = AsrSessionState.Connected("https://asr.example.com", "session-1")
        val engine = createEngine(backgroundScope)
        runCurrent()

        engine.startListening()
        runCurrent()

        engine.interruptListening()
        runCurrent()

        verify { mapboxASRService.sendFinalAsrData(true) }
    }

    @Test
    fun `when Listening times out with blank transcript then engine emits InterruptedByTimeout`() =
        testScope.runBlockingTest {
            sessionStateFlow.value =
                AsrSessionState.Connected("https://asr.example.com", "session-1")
            val engine = createEngine(
                scope = backgroundScope,
                stoppedSpeakingThreshold = 0.milliseconds,
                checkSpeakingInterval = 1.milliseconds,
            )
            runCurrent()

            engine.startListening()
            runCurrent()

            advanceTimeBy(10)
            runCurrent()

            val state = engine.state.value
            Assert.assertTrue(state is ASRState.InterruptedByTimeout)
        }

    @Test
    fun `when SpeechFinishedWaitingForResult times out then engine emits InterruptedByTimeout`() =
        testScope.runBlockingTest {
            sessionStateFlow.value =
                AsrSessionState.Connected("https://asr.example.com", "session-1")
            val engine = createEngine(
                scope = backgroundScope,
                resultTimeout = 100.milliseconds,
                checkSpeakingInterval = 1.milliseconds,
            )
            runCurrent()

            engine.startListening()
            runCurrent()

            launch {
                asrDataFlow.emit(AsrData.Transcript("speech", isFinal = true))
            }
            runCurrent()

            Assert.assertTrue(engine.state.value is ASRState.SpeechFinishedWaitingForResult)

            advanceTimeBy(150)
            runCurrent()

            val state = engine.state.value
            Assert.assertTrue(state is ASRState.InterruptedByTimeout)
        }

    @Test
    fun `when connect invoked then delegate to mapboxASRService`() = testScope.runBlockingTest {
        sessionStateFlow.value = AsrSessionState.Connected("https://asr.example.com", "session-1")
        val engine = createEngine(backgroundScope)
        runCurrent()

        engine.connect("test-token")

        verify { mapboxASRService.connect("test-token") }
    }

    @Test
    fun `when disconnect invoked then delegate to mapboxASRService`() = testScope.runBlockingTest {
        sessionStateFlow.value = AsrSessionState.Connected("https://asr.example.com", "session-1")
        val engine = createEngine(backgroundScope)
        runCurrent()

        engine.disconnect()
        advanceTimeBy(10)
        runCurrent()

        coVerify { mapboxASRService.disconnect() }
    }
}

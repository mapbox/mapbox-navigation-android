package com.mapbox.navigation.ui.androidauto.navigation.audioguidance

import android.content.Context
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.mapbox.common.dispatchers.SdkDispatchersTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.navigation.voice.api.MapboxAudioGuidance
import com.mapbox.navigation.voice.api.MapboxAudioGuidanceState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CarAudioGuidanceActionTest : MapboxRobolectricTestRunner() {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val sdkDispatchersRule = SdkDispatchersTestRule(dispatcher)

    private val stateFlow = MutableStateFlow(audioGuidanceState(isMuted = false))
    private val audioGuidance: MapboxAudioGuidance = mockk(relaxed = true) {
        every { stateFlow() } returns this@CarAudioGuidanceActionTest.stateFlow
    }

    @Before
    fun setUp() {
        mockkObject(MapboxAudioGuidance.Companion)
        every { MapboxAudioGuidance.getRegisteredInstance() } returns audioGuidance
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `every screen refreshes its own action`() {
        val firstScreen = screen()
        val secondScreen = screen()

        CarAudioGuidanceAction().getAction(firstScreen.screen)
        CarAudioGuidanceAction().getAction(secondScreen.screen)
        stateFlow.value = audioGuidanceState(isMuted = true)

        verify(exactly = 1) { firstScreen.screen.invalidate() }
        verify(exactly = 1) { secondScreen.screen.invalidate() }
    }

    @Test
    fun `rebuilding the action refreshes the screen once per state change`() {
        val screen = screen()

        repeat(3) { CarAudioGuidanceAction().getAction(screen.screen) }
        stateFlow.value = audioGuidanceState(isMuted = true)
        CarAudioGuidanceAction().getAction(screen.screen)
        stateFlow.value = audioGuidanceState(isMuted = false)

        verify(exactly = 2) { screen.screen.invalidate() }
    }

    @Test
    fun `building the action does not refresh the screen`() {
        val screen = screen()

        CarAudioGuidanceAction().getAction(screen.screen)

        verify(exactly = 0) { screen.screen.invalidate() }
    }

    @Test
    fun `a stopped screen is not refreshed`() {
        val screen = screen()
        CarAudioGuidanceAction().getAction(screen.screen)

        screen.owner.currentState = Lifecycle.State.CREATED
        stateFlow.value = audioGuidanceState(isMuted = true)

        verify(exactly = 0) { screen.screen.invalidate() }
    }

    @Test
    fun `a destroyed screen is not refreshed`() {
        val screen = screen()
        CarAudioGuidanceAction().getAction(screen.screen)

        screen.owner.currentState = Lifecycle.State.DESTROYED
        stateFlow.value = audioGuidanceState(isMuted = true)

        verify(exactly = 0) { screen.screen.invalidate() }
    }

    private class TestScreen(val screen: Screen, val owner: TestLifecycleOwner)

    private fun screen(): TestScreen {
        val owner = TestLifecycleOwner(Lifecycle.State.RESUMED, dispatcher)
        val carContext: CarContext = mockk {
            every { resources } returns context.resources
            every { packageName } returns context.packageName
        }
        val screen: Screen = mockk {
            every { lifecycle } returns owner.lifecycle
            every { this@mockk.carContext } returns carContext
            every { invalidate() } just runs
        }
        return TestScreen(screen, owner)
    }

    private fun audioGuidanceState(isMuted: Boolean): MapboxAudioGuidanceState = mockk {
        every { this@mockk.isMuted } returns isMuted
        every { isPlayable } returns true
    }
}

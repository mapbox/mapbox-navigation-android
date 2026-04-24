package com.mapbox.navigation.ui.voicefeedback.view

import androidx.fragment.app.FragmentActivity
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.voicefeedback.ASRState
import com.mapbox.navigation.voicefeedback.FeedbackAgentSession
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class MapboxVoiceFeedbackDialogTest {

    private lateinit var activityController: ActivityController<FragmentActivity>
    private lateinit var mockSession: FeedbackAgentSession
    private lateinit var asrStateFlow: MutableStateFlow<ASRState?>

    @Before
    fun setUp() {
        mockkConstructor(FeedbackAgentSession.Builder::class)
        asrStateFlow = MutableStateFlow(ASRState.Idle)
        val builder = mockk<FeedbackAgentSession.Builder>()
        mockSession = mockk(relaxUnitFun = true)
        every { mockSession.asrState } returns asrStateFlow
        every { anyConstructed<FeedbackAgentSession.Builder>().build() } returns mockSession
        every { anyConstructed<FeedbackAgentSession.Builder>().options(any()) } returns builder
        every { builder.options(any()) } returns builder
        every { builder.build() } returns mockSession

        activityController = Robolectric.buildActivity(FragmentActivity::class.java)
        activityController.create().start().resume()
    }

    @After
    fun tearDown() {
        activityController.pause().stop().destroy()
        unmockkConstructor(FeedbackAgentSession.Builder::class)
    }

    @Test
    fun `startListening not called when session already in non-idle state`() {
        asrStateFlow.value = ASRState.Listening("test")
        every { mockSession.startListening() } just Runs

        val activity = activityController.get()
        MapboxVoiceFeedbackDialog().show(
            activity.supportFragmentManager,
            MapboxVoiceFeedbackDialog.TAG
        )

        // startListening should NOT be called because current state is Listening, not Idle
        verify(exactly = 0) { mockSession.startListening() }
    }

    @Test
    fun `startListening called when session is in idle state`() {
        asrStateFlow.value = ASRState.Idle
        every { mockSession.startListening() } just Runs

        val activity = activityController.get()
        MapboxVoiceFeedbackDialog().show(
            activity.supportFragmentManager,
            MapboxVoiceFeedbackDialog.TAG
        )

        // Advance main looper to let coroutine execute
        Shadows.shadowOf(activity.mainLooper).idle()

        // startListening should be called because current state is Idle
        verify { mockSession.startListening() }
    }
}

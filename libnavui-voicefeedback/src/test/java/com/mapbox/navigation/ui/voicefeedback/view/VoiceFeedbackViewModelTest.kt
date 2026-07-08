package com.mapbox.navigation.ui.voicefeedback.view

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.voicefeedback.FeedbackAgentSession
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class VoiceFeedbackViewModelTest {

    private lateinit var viewModel: VoiceFeedbackViewModel
    private lateinit var mockSession: FeedbackAgentSession

    @Before
    fun setUp() {
        mockkConstructor(FeedbackAgentSession.Builder::class)
        val builder = mockk<FeedbackAgentSession.Builder>()
        mockSession = mockk(relaxUnitFun = true)
        every { anyConstructed<FeedbackAgentSession.Builder>().build() } returns mockSession
        every { anyConstructed<FeedbackAgentSession.Builder>().options(any()) } returns builder
        every { builder.options(any()) } returns builder
        every { builder.build() } returns mockSession

        viewModel = VoiceFeedbackViewModel()
    }

    @After
    fun tearDown() {
        unmockkConstructor(FeedbackAgentSession.Builder::class)
    }

    @Test
    fun `attach calls session onAttached and connect`() {
        every { mockSession.onAttached(any()) } just Runs
        every { mockSession.connect() } just Runs
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

        viewModel.attach(mapboxNavigation)

        verify { mockSession.onAttached(mapboxNavigation) }
        verify { mockSession.connect() }
    }

    @Test
    fun `detach calls session onDetached`() {
        every { mockSession.onDetached(any()) } just Runs
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

        viewModel.detach(mapboxNavigation)

        verify { mockSession.onDetached(mapboxNavigation) }
    }

    @Test
    fun `session is created and accessible`() {
        val session = viewModel.session

        assert(session === mockSession)
    }
}

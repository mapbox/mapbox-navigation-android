package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.AnimatorSet
import com.mapbox.maps.CameraOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NavigationCameraStateTransitionWrapperTest {

    private val mockStateTransition = mockk<NavigationCameraStateTransition>()
    private val mockCameraOptions = mockk<CameraOptions>()
    private val mockTransitionOptions = mockk<NavigationCameraTransitionOptions>()
    private val mockAnimatorSet = mockk<AnimatorSet>()

    private lateinit var wrapper: NavigationCameraStateTransitionWrapper

    @Before
    fun setup() {
        wrapper = NavigationCameraStateTransitionWrapper(mockStateTransition)
    }

    @Test
    fun `transitionToFollowing delegates to underlying state transition`() {
        every {
            mockStateTransition.transitionToFollowing(mockCameraOptions, mockTransitionOptions)
        } returns mockAnimatorSet

        val result = wrapper.transitionToFollowing(mockCameraOptions, mockTransitionOptions)

        verify {
            mockStateTransition.transitionToFollowing(mockCameraOptions, mockTransitionOptions)
        }
        assertEquals(mockAnimatorSet, result)
    }

    @Test
    fun `transitionToOverview delegates to underlying state transition`() {
        every {
            mockStateTransition.transitionToOverview(mockCameraOptions, mockTransitionOptions)
        } returns mockAnimatorSet

        val result = wrapper.transitionToOverview(mockCameraOptions, mockTransitionOptions)

        verify {
            mockStateTransition.transitionToOverview(mockCameraOptions, mockTransitionOptions)
        }
        assertEquals(mockAnimatorSet, result)
    }

    @Test
    fun `multiple calls to transitionToFollowing work correctly`() {
        val animatorSet1 = mockk<AnimatorSet>()
        val animatorSet2 = mockk<AnimatorSet>()
        val cameraOptions1 = mockk<CameraOptions>()
        val cameraOptions2 = mockk<CameraOptions>()
        val transitionOptions1 = mockk<NavigationCameraTransitionOptions>()
        val transitionOptions2 = mockk<NavigationCameraTransitionOptions>()

        every {
            mockStateTransition.transitionToFollowing(cameraOptions1, transitionOptions1)
        } returns animatorSet1
        every {
            mockStateTransition.transitionToFollowing(cameraOptions2, transitionOptions2)
        } returns animatorSet2

        val result1 = wrapper.transitionToFollowing(cameraOptions1, transitionOptions1)
        val result2 = wrapper.transitionToFollowing(cameraOptions2, transitionOptions2)

        verify { mockStateTransition.transitionToFollowing(cameraOptions1, transitionOptions1) }
        verify { mockStateTransition.transitionToFollowing(cameraOptions2, transitionOptions2) }
        assertEquals(animatorSet1, result1)
        assertEquals(animatorSet2, result2)
    }

    @Test
    fun `multiple calls to transitionToOverview work correctly`() {
        val animatorSet1 = mockk<AnimatorSet>()
        val animatorSet2 = mockk<AnimatorSet>()
        val cameraOptions1 = mockk<CameraOptions>()
        val cameraOptions2 = mockk<CameraOptions>()
        val transitionOptions1 = mockk<NavigationCameraTransitionOptions>()
        val transitionOptions2 = mockk<NavigationCameraTransitionOptions>()

        every {
            mockStateTransition.transitionToOverview(cameraOptions1, transitionOptions1)
        } returns animatorSet1
        every {
            mockStateTransition.transitionToOverview(cameraOptions2, transitionOptions2)
        } returns animatorSet2

        val result1 = wrapper.transitionToOverview(cameraOptions1, transitionOptions1)
        val result2 = wrapper.transitionToOverview(cameraOptions2, transitionOptions2)

        verify { mockStateTransition.transitionToOverview(cameraOptions1, transitionOptions1) }
        verify { mockStateTransition.transitionToOverview(cameraOptions2, transitionOptions2) }
        assertEquals(animatorSet1, result1)
        assertEquals(animatorSet2, result2)
    }
}

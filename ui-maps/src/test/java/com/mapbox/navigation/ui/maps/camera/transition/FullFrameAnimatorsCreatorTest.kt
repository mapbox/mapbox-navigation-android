package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import com.mapbox.maps.CameraOptions
import com.mapbox.navigation.testing.assertIs
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

internal class FullFrameAnimatorsCreatorTest {

    private val children = arrayListOf<Animator>(mockk<ValueAnimator>(), mockk<ValueAnimator>())
    private val animatorSet = mockk<AnimatorSet> {
        every { childAnimations } returns children
    }
    private val cameraOptions = mockk<CameraOptions>()
    private val transitionOptions = mockk<NavigationCameraTransitionOptions>()
    private val stateTransition = mockk<NavigationCameraStateTransition>(relaxed = true)

    private val animatorsCreator = FullFrameAnimatorsCreator(stateTransition)

    @Test
    fun transitionToFollowing() {
        every {
            stateTransition.transitionToFollowing(cameraOptions, transitionOptions)
        } returns animatorSet

        val actual = animatorsCreator.transitionToFollowing(cameraOptions, transitionOptions)

        assertIs<FullAnimatorSet>(actual)
        assertEquals(children, actual.children)
    }

    @Test
    fun transitionToOverview() {
        every {
            stateTransition.transitionToOverview(cameraOptions, transitionOptions)
        } returns animatorSet

        val actual = animatorsCreator.transitionToOverview(cameraOptions, transitionOptions)

        assertIs<FullAnimatorSet>(actual)
        assertEquals(children, actual.children)
    }

    @Test
    fun updateFrameForFollowing() {
        every {
            stateTransition.updateFrameForFollowing(cameraOptions, transitionOptions)
        } returns animatorSet

        val actual = animatorsCreator.updateFrameForFollowing(cameraOptions, transitionOptions)

        assertIs<FullAnimatorSet>(actual)
        assertEquals(children, actual.children)
    }

    @Test
    fun updateFrameForOverview() {
        every {
            stateTransition.updateFrameForOverview(cameraOptions, transitionOptions)
        } returns animatorSet

        val actual = animatorsCreator.updateFrameForOverview(cameraOptions, transitionOptions)

        assertIs<FullAnimatorSet>(actual)
        assertEquals(children, actual.children)
    }
}

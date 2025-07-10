package com.mapbox.navigation.ui.maps.camera.transition

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.navigation.testing.assertIs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

internal class FullFrameAnimatorsCreatorTest {

    private val children = arrayListOf<Animator>(mockk<ValueAnimator>(), mockk<ValueAnimator>())
    private val animatorSet = mockk<AnimatorSet>(relaxed = true) {
        every { childAnimations } returns children
    }
    private val cameraOptions = mockk<CameraOptions>()
    private val transitionOptions = mockk<NavigationCameraTransitionOptions>()
    private val stateTransition = mockk<NavigationCameraStateTransition>(relaxed = true)
    private val cameraAnimationsPlugin = mockk<CameraAnimationsPlugin>(relaxed = true)

    private val animatorsCreator = FullFrameAnimatorsCreator(
        stateTransition,
        cameraAnimationsPlugin,
        mockk(relaxed = true),
    )

    @Test
    fun transitionToFollowing() {
        every {
            stateTransition.transitionToFollowing(cameraOptions, transitionOptions)
        } returns animatorSet

        val actual = animatorsCreator.transitionToFollowing(cameraOptions, transitionOptions)

        checkChildAnimators(animatorSet, actual)
    }

    @Test
    fun transitionToOverview() {
        every {
            stateTransition.transitionToOverview(cameraOptions, transitionOptions)
        } returns animatorSet

        val actual = animatorsCreator.transitionToOverview(cameraOptions, transitionOptions)

        checkChildAnimators(animatorSet, actual)
    }

    @Test
    fun updateFrameForFollowing() {
        every {
            stateTransition.updateFrameForFollowing(cameraOptions, transitionOptions)
        } returns animatorSet

        val actual = animatorsCreator.updateFrameForFollowing(cameraOptions, transitionOptions)

        assertIs<FullAnimatorSet>(actual)
        checkChildAnimators(animatorSet, actual as FullAnimatorSet)
    }

    @Test
    fun updateFrameForOverview() {
        every {
            stateTransition.updateFrameForOverview(cameraOptions, transitionOptions)
        } returns animatorSet

        val actual = animatorsCreator.updateFrameForOverview(cameraOptions, transitionOptions)

        assertIs<FullAnimatorSet>(actual)
        checkChildAnimators(animatorSet, actual as FullAnimatorSet)
    }

    private fun checkChildAnimators(expected: AnimatorSet, animatorSet: FullAnimatorSet) {
        animatorSet.start()
        verify { expected.start() }
    }
}

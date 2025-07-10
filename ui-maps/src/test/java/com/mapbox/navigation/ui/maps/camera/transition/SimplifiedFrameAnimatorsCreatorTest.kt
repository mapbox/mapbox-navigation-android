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

internal class SimplifiedFrameAnimatorsCreatorTest {

    private val children = arrayListOf<Animator>(mockk<ValueAnimator>(), mockk<ValueAnimator>())
    private val updateFrameChildren = arrayListOf(
        mockk<ValueAnimator>(relaxed = true),
        mockk<ValueAnimator>(relaxed = true),
    )
    private val animatorSet = mockk<AnimatorSet>(relaxed = true) {
        every { childAnimations } returns children
    }
    private val cameraOptions = mockk<CameraOptions>()
    private val transitionOptions = mockk<NavigationCameraTransitionOptions>()
    private val stateTransition = mockk<NavigationCameraStateTransition>(relaxed = true)
    private val cameraAnimationsPlugin = mockk<CameraAnimationsPlugin>(relaxed = true)
    private val simplifiedUpdateFrameTransition =
        mockk<DefaultSimplifiedUpdateFrameTransitionProvider>(relaxed = true)

    private val animatorsCreator = SimplifiedFrameAnimatorsCreator(
        cameraAnimationsPlugin,
        mockk(relaxed = true),
        stateTransition,
        simplifiedUpdateFrameTransition,
    )

    @Test
    fun transitionToFollowing() {
        every {
            stateTransition.transitionToFollowing(cameraOptions, transitionOptions)
        } returns animatorSet

        val actual = animatorsCreator.transitionToFollowing(cameraOptions, transitionOptions)

        actual.start()
        verify { animatorSet.start() }
    }

    @Test
    fun transitionToOverview() {
        every {
            stateTransition.transitionToOverview(cameraOptions, transitionOptions)
        } returns animatorSet

        val actual = animatorsCreator.transitionToOverview(cameraOptions, transitionOptions)

        actual.start()
        verify { animatorSet.start() }
    }

    @Test
    fun updateFrameForFollowing() {
        every {
            simplifiedUpdateFrameTransition.updateFollowingFrame(cameraOptions, transitionOptions)
        } returns updateFrameChildren

        val actual = animatorsCreator.updateFrameForFollowing(cameraOptions, transitionOptions)

        assertIs<SimplifiedAnimatorSet>(actual)
        checkChildAnimators(updateFrameChildren, actual)
    }

    @Test
    fun updateFrameForOverview() {
        every {
            simplifiedUpdateFrameTransition.updateOverviewFrame(cameraOptions, transitionOptions)
        } returns updateFrameChildren

        val actual = animatorsCreator.updateFrameForOverview(cameraOptions, transitionOptions)

        assertIs<SimplifiedAnimatorSet>(actual)
        checkChildAnimators(updateFrameChildren, actual)
    }

    private fun checkChildAnimators(expected: List<Animator>, animatorSet: MapboxAnimatorSet) {
        animatorSet.start()
        expected.forEach {
            verify { it.start() }
        }
    }
}

package com.mapbox.navigation.ui.maps.camera.lifecycle

import android.graphics.RectF
import com.mapbox.android.gestures.AndroidGesturesManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.android.gestures.ShoveGestureDetector
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GestureThresholdsControllerTest {

    private val gesturesManager: AndroidGesturesManager = mockk()
    private val moveGestureDetector: MoveGestureDetector = mockk()
    private val rotateGestureDetector: RotateGestureDetector = mockk()
    private val shoveGestureDetector: ShoveGestureDetector = mockk()
    private val baselineMoveThreshold = 1f
    private val baselineMultiFingerMoveThreshold = 2f
    private val baselineMoveThresholdRect = RectF(1f, 2f, 3f, 4f)
    private val baselineRotationAngleThreshold = 3f
    private val baselineShovePixelDeltaThreshold = 4f

    @Before
    fun setUp() {
        every { gesturesManager.moveGestureDetector } returns moveGestureDetector
        every { gesturesManager.rotateGestureDetector } returns rotateGestureDetector
        every { gesturesManager.shoveGestureDetector } returns shoveGestureDetector
        every { moveGestureDetector.moveThreshold } returns baselineMoveThreshold
        every { moveGestureDetector.multiFingerMoveThreshold } returns
            baselineMultiFingerMoveThreshold
        every { moveGestureDetector.moveThresholdRect } returns baselineMoveThresholdRect
        every { rotateGestureDetector.angleThreshold } returns baselineRotationAngleThreshold
        every { shoveGestureDetector.pixelDeltaThreshold } returns baselineShovePixelDeltaThreshold
    }

    @Test
    fun `inactive stale owner cannot restore thresholds after another handler takes ownership`() {
        val firstOwner = Any()
        val secondOwner = Any()
        GestureThresholdsController.applyFollowing(gesturesManager, firstOwner) {}
        GestureThresholdsController.applyFollowing(gesturesManager, secondOwner) {}

        var staleOwnerRestored = false
        GestureThresholdsController.restoreBaseline(gesturesManager, firstOwner, false) {
            staleOwnerRestored = true
        }

        assertFalse(staleOwnerRestored)

        var currentOwnerRestored = false
        GestureThresholdsController.restoreBaseline(
            gesturesManager,
            secondOwner,
            false,
        ) { thresholds ->
            currentOwnerRestored = true
            assertBaseline(thresholds)
        }

        assertTrue(currentOwnerRestored)
    }

    @Test
    fun `active handler restores current baseline when no handler owns thresholds`() {
        var restored = false

        GestureThresholdsController.restoreBaseline(
            gesturesManager,
            Any(),
            true,
        ) { thresholds ->
            restored = true
            assertBaseline(thresholds)
        }

        assertTrue(restored)
    }

    @Test
    fun `inactive handler restores baseline only while it owns thresholds`() {
        val owner = Any()
        GestureThresholdsController.applyFollowing(gesturesManager, owner) {}

        var restored = false
        GestureThresholdsController.restoreBaseline(
            gesturesManager,
            owner,
            false,
        ) { thresholds ->
            restored = true
            assertBaseline(thresholds)
        }

        assertTrue(restored)
    }

    private fun assertBaseline(thresholds: GestureThresholdsController.ThresholdsState) {
        assertEquals(baselineMoveThreshold, thresholds.moveThreshold)
        assertEquals(baselineMultiFingerMoveThreshold, thresholds.multiFingerMoveThreshold)
        val moveThresholdRect = requireNotNull(thresholds.moveThresholdRect)
        assertEquals(baselineMoveThresholdRect.left, moveThresholdRect.left)
        assertEquals(baselineMoveThresholdRect.top, moveThresholdRect.top)
        assertEquals(baselineMoveThresholdRect.right, moveThresholdRect.right)
        assertEquals(baselineMoveThresholdRect.bottom, moveThresholdRect.bottom)
        assertEquals(baselineRotationAngleThreshold, thresholds.rotationAngleThreshold)
        assertEquals(baselineShovePixelDeltaThreshold, thresholds.shovePixelDeltaThreshold)
    }
}

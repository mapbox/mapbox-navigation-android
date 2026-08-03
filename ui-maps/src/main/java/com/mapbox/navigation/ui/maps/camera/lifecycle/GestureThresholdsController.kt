package com.mapbox.navigation.ui.maps.camera.lifecycle

import android.graphics.RectF
import com.mapbox.android.gestures.AndroidGesturesManager
import java.util.WeakHashMap

/**
 * Coordinates threshold writes from handlers that share an [AndroidGesturesManager].
 *
 * When a camera becomes inactive, its handler must restore the gesture manager's baseline
 * thresholds. Multiple handlers can share that manager, however. If a newly active handler has
 * already applied its following thresholds, a delayed callback from the inactive handler must not
 * restore the baseline over those values. This controller records the handler that last applied
 * following thresholds and lets only that owner restore them.
 *
 * A state entry exists while a handler owns following thresholds; it may also be created transiently
 * to snapshot the current baseline for an active non-following handler. Entries are removed when
 * the baseline is restored. The registry uses weak manager keys and [ThresholdsState] does not
 * reference its manager, so this controller does not prevent an unused
 * [AndroidGesturesManager] from being garbage collected. Callers must use an owner token that
 * likewise does not retain the manager.
 *
 * All public operations are synchronized on this object. Ownership changes and their [action]
 * execute atomically, preventing concurrent callbacks from interleaving threshold writes. Keep
 * actions short and non-blocking because they run while the controller monitor is held.
 */
internal object GestureThresholdsController {

    /**
     * Baseline threshold values captured before a handler applies following thresholds.
     *
     * [owner] identifies the handler currently allowed to restore this baseline.
     */
    internal data class ThresholdsState(
        val moveThreshold: Float,
        val multiFingerMoveThreshold: Float,
        val moveThresholdRect: RectF?,
        val rotationAngleThreshold: Float,
        val shovePixelDeltaThreshold: Float,
        var owner: Any? = null,
    )

    private val states = WeakHashMap<AndroidGesturesManager, ThresholdsState>()

    /**
     * Records [owner] as the current threshold owner for [gesturesManager] and runs [action] to
     * apply its following thresholds.
     *
     * The baseline is captured only for the first owner. A subsequent owner of the same manager
     * reuses that baseline, so it can still restore the original values when it leaves following.
     */
    @Synchronized
    fun applyFollowing(
        gesturesManager: AndroidGesturesManager,
        owner: Any,
        action: () -> Unit,
    ) {
        states.getOrPut(gesturesManager) { thresholdsState(gesturesManager) }.owner = owner
        action()
    }

    /**
     * Runs [action] with [gesturesManager]'s baseline thresholds when [owner] may restore them.
     *
     * The current owner may always restore its baseline. When no handler owns thresholds,
     * an active handler may also restore the current baseline; this retains normal non-following
     * behavior. An inactive non-owner is ignored, preventing a delayed callback from overwriting
     * thresholds applied by another handler. A successful restoration removes the state entry.
     */
    @Synchronized
    fun restoreBaseline(
        gesturesManager: AndroidGesturesManager,
        owner: Any,
        isActive: Boolean,
        action: (ThresholdsState) -> Unit,
    ) {
        val state = states[gesturesManager] ?: if (isActive) {
            thresholdsState(gesturesManager).also { states[gesturesManager] = it }
        } else {
            return
        }
        val ownsThresholds = state.owner === owner
        if (!ownsThresholds && !(isActive && state.owner == null)) {
            return
        }

        action(state)
        states.remove(gesturesManager)
    }

    /**
     * Captures the current baseline thresholds from [gesturesManager].
     *
     * This is called only while the controller monitor is held.
     */
    private fun thresholdsState(gesturesManager: AndroidGesturesManager): ThresholdsState {
        return ThresholdsState(
            moveThreshold = gesturesManager.moveGestureDetector.moveThreshold,
            multiFingerMoveThreshold = gesturesManager.moveGestureDetector.multiFingerMoveThreshold,
            moveThresholdRect = gesturesManager.moveGestureDetector.moveThresholdRect,
            rotationAngleThreshold = gesturesManager.rotateGestureDetector.angleThreshold,
            shovePixelDeltaThreshold = gesturesManager.shoveGestureDetector.pixelDeltaThreshold,
        )
    }
}

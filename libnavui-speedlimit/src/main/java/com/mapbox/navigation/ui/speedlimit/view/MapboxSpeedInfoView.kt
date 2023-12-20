package com.mapbox.navigation.ui.speedlimit.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.BOTTOM
import androidx.constraintlayout.widget.ConstraintSet.END
import androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
import androidx.constraintlayout.widget.ConstraintSet.START
import androidx.constraintlayout.widget.ConstraintSet.TOP
import androidx.core.widget.TextViewCompat
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedInfoApi
import com.mapbox.navigation.ui.speedlimit.databinding.MapboxSpeedInfoViewBinding
import com.mapbox.navigation.ui.speedlimit.model.CurrentSpeedDirection
import com.mapbox.navigation.ui.speedlimit.model.MapboxSpeedInfoOptions
import com.mapbox.navigation.ui.speedlimit.model.SpeedInfoValue
import com.mapbox.navigation.ui.speedlimit.model.ViewConstraints
import com.mapbox.navigation.utils.internal.android.isVisible
import com.mapbox.navigation.utils.internal.android.updateLayoutParams

/**
 * A view component responsible to render posted speed limit and current speed produced by
 * [MapboxSpeedInfoApi].
 */
@UiThread
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxSpeedInfoView : FrameLayout {

    /**
     * @param context Context
     * @constructor
     */
    constructor(context: Context) : super(context)

    /**
     * @param context Context
     * @param attrs AttributeSet?
     * @constructor
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     * @param context Context
     * @param attrs AttributeSet?
     * @param defStyleAttr Int
     * @constructor
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    private val binding: MapboxSpeedInfoViewBinding = MapboxSpeedInfoViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    /**
     * [MapboxSpeedInfoOptions] currently applied to [MapboxSpeedInfoView]
     */
    var speedInfoOptions: MapboxSpeedInfoOptions = MapboxSpeedInfoOptions.Builder().build()

    /**
     * Root MUTCD layout.
     */
    val speedInfoMutcdLayout: ConstraintLayout = binding.speedInfoMutcdLayout

    /**
     * Root MUTCD layout that renders posted speed legend, posted speed and posted speed unit child
     * views.
     */
    val speedInfoPostedSpeedLayoutMutcd: ConstraintLayout = binding.postedSpeedLayoutMutcd

    /**
     * [AppCompatTextView] that renders MUTCD posted speed legend.
     */
    val speedInfoLegendTextMutcd: AppCompatTextView = binding.postedSpeedLegend

    /**
     * [AppCompatTextView] that renders MUTCD posted speed.
     */
    val speedInfoPostedSpeedMutcd: AppCompatTextView = binding.postedSpeedMutcd

    /**
     * [AppCompatTextView] that renders MUTCD posted speed unit.
     */
    val speedInfoUnitTextMutcd: AppCompatTextView = binding.postedSpeedUnit

    /**
     * [AppCompatTextView] that renders MUTCD current speed.
     */
    val speedInfoCurrentSpeedMutcd: AppCompatTextView = binding.currentSpeedMutcd

    /**
     * Root VIENNA layout.
     */
    val speedInfoViennaLayout: ConstraintLayout = binding.speedInfoViennaLayout

    /**
     * Root VIENNA layout that renders posted speed child view.
     */
    val speedInfoPostedSpeedLayoutVienna: ConstraintLayout = binding.postedSpeedLayoutVienna

    /**
     * [AppCompatTextView] that renders VIENNA posted speed.
     */
    val speedInfoPostedSpeedVienna: AppCompatTextView = binding.postedSpeedVienna

    /**
     * [AppCompatTextView] that renders VIENNA current speed.
     */
    val speedInfoCurrentSpeedVienna: AppCompatTextView = binding.currentSpeedVienna

    internal var speedInfo: SpeedInfoValue? = null

    init {
        applyOptions(speedInfoOptions)
        updateStyles()
    }

    /**
     * Updates this view with posted speed limit and current speed related data.
     *
     * @param speedInfo SpeedInfoValue
     */
    fun render(speedInfo: SpeedInfoValue) {
        this.speedInfo = speedInfo
        renderSpeedUnit(speedInfo.postedSpeedUnit)
        if (speedInfo.postedSpeed != null) {
            showMutcdOrVienna(speedInfo.speedSignConvention)
            speedInfoPostedSpeedMutcd.text = speedInfo.postedSpeed.toString()
            speedInfoCurrentSpeedMutcd.text = speedInfo.currentSpeed.toString()
            speedInfoCurrentSpeedMutcd.isVisible =
                speedInfo.currentSpeed > speedInfo.postedSpeed && speedInfo.postedSpeed > 0

            speedInfoPostedSpeedVienna.text = speedInfo.postedSpeed.toString()
            speedInfoCurrentSpeedVienna.text = speedInfo.currentSpeed.toString()
            speedInfoCurrentSpeedVienna.isVisible =
                speedInfo.currentSpeed > speedInfo.postedSpeed && speedInfo.postedSpeed > 0
        } else {
            if (speedInfoOptions.showSpeedWhenUnavailable) {
                showMutcdOrVienna(speedInfo.speedSignConvention)
                speedInfoPostedSpeedMutcd.text = "--"
                speedInfoCurrentSpeedMutcd.text = ""
                speedInfoCurrentSpeedMutcd.isVisible = false

                speedInfoPostedSpeedVienna.text = "--"
                speedInfoCurrentSpeedVienna.text = ""
                speedInfoCurrentSpeedVienna.isVisible = false
            } else {
                speedInfoMutcdLayout.isVisible = false
                speedInfoViennaLayout.isVisible = false
            }
        }
    }

    /**
     * Apply runtime changes to [MapboxSpeedLimitView] using [MapboxSpeedInfoOptions]
     * @param speedInfoOptions
     */
    fun applyOptions(speedInfoOptions: MapboxSpeedInfoOptions) {
        this.speedInfoOptions = speedInfoOptions
        updateStyles()
        updateView()
    }

    private fun showMutcdOrVienna(speedSignConvention: SpeedLimitSign?) {
        when (speedSignConvention) {
            SpeedLimitSign.MUTCD -> {
                speedInfoMutcdLayout.isVisible =
                    speedInfoOptions.renderWithSpeedSign != SpeedLimitSign.VIENNA
                speedInfoViennaLayout.isVisible =
                    !speedInfoMutcdLayout.isVisible
            }
            SpeedLimitSign.VIENNA -> {
                speedInfoViennaLayout.isVisible =
                    speedInfoOptions.renderWithSpeedSign != SpeedLimitSign.MUTCD
                speedInfoMutcdLayout.isVisible =
                    !speedInfoViennaLayout.isVisible
            }
            // TODO: remove the null check after NN-305 is available
            null -> {
                when (speedInfoOptions.renderWithSpeedSign) {
                    null -> {
                        // no op
                    }
                    SpeedLimitSign.MUTCD -> {
                        speedInfoMutcdLayout.isVisible = true
                        speedInfoViennaLayout.isVisible = false
                    }
                    SpeedLimitSign.VIENNA -> {
                        speedInfoMutcdLayout.isVisible = false
                        speedInfoViennaLayout.isVisible = true
                    }
                }
            }
        }
    }

    private fun renderSpeedUnit(speedUnit: SpeedUnit) {
        when (speedUnit) {
            SpeedUnit.MILES_PER_HOUR -> {
                speedInfoUnitTextMutcd.text = "mph"
            }
            SpeedUnit.KILOMETERS_PER_HOUR -> {
                speedInfoUnitTextMutcd.text = "km/h"
            }
            else -> {
                // No-op
            }
        }
    }

    private fun updateStyles() {
        speedInfoMutcdLayout.setBackgroundResource(
            speedInfoOptions.speedInfoStyle.mutcdLayoutBackground
        )
        speedInfoPostedSpeedLayoutMutcd.setBackgroundResource(
            speedInfoOptions.speedInfoStyle.postedSpeedMutcdLayoutBackground
        )
        TextViewCompat.setTextAppearance(
            speedInfoLegendTextMutcd,
            speedInfoOptions.speedInfoStyle.postedSpeedLegendTextAppearance
        )
        TextViewCompat.setTextAppearance(
            speedInfoPostedSpeedMutcd,
            speedInfoOptions.speedInfoStyle.postedSpeedMutcdTextAppearance
        )
        TextViewCompat.setTextAppearance(
            speedInfoUnitTextMutcd,
            speedInfoOptions.speedInfoStyle.postedSpeedUnitTextAppearance
        )
        TextViewCompat.setTextAppearance(
            speedInfoCurrentSpeedMutcd,
            speedInfoOptions.speedInfoStyle.currentSpeedMutcdTextAppearance
        )

        speedInfoViennaLayout.setBackgroundResource(
            speedInfoOptions.speedInfoStyle.viennaLayoutBackground
        )
        speedInfoPostedSpeedLayoutVienna.setBackgroundResource(
            speedInfoOptions.speedInfoStyle.postedSpeedViennaLayoutBackground
        )
        TextViewCompat.setTextAppearance(
            speedInfoPostedSpeedVienna,
            speedInfoOptions.speedInfoStyle.postedSpeedViennaTextAppearance
        )
        TextViewCompat.setTextAppearance(
            speedInfoCurrentSpeedVienna,
            speedInfoOptions.speedInfoStyle.currentSpeedViennaTextAppearance
        )
    }

    private fun updateView() {
        speedInfoUnitTextMutcd.isVisible = speedInfoOptions.showUnit
        speedInfoLegendTextMutcd.isVisible = speedInfoOptions.showLegend
        when (speedInfoOptions.currentSpeedDirection) {
            CurrentSpeedDirection.TOP -> {
                renderCurrentSpeedToTop()
            }
            CurrentSpeedDirection.END -> {
                renderCurrentSpeedToEnd()
            }
            CurrentSpeedDirection.START -> {
                renderCurrentSpeedToStart()
            }
            CurrentSpeedDirection.BOTTOM -> {
                renderCurrentSpeedToBottom()
            }
        }
    }

    private fun renderCurrentSpeedToTop() {
        val currentSpeedMutcdId = speedInfoCurrentSpeedMutcd.id
        val postedSpeedMutcdId = speedInfoPostedSpeedLayoutMutcd.id
        val mutcdConstraints = listOf(
            ViewConstraints(postedSpeedMutcdId, END, PARENT_ID, END),
            ViewConstraints(postedSpeedMutcdId, START, PARENT_ID, START),
            ViewConstraints(postedSpeedMutcdId, BOTTOM, PARENT_ID, BOTTOM),
            ViewConstraints(viewId = postedSpeedMutcdId, anchor = TOP, shouldConnect = false),
            ViewConstraints(currentSpeedMutcdId, TOP, PARENT_ID, TOP),
            ViewConstraints(currentSpeedMutcdId, END, postedSpeedMutcdId, END),
            ViewConstraints(currentSpeedMutcdId, BOTTOM, postedSpeedMutcdId, TOP),
            ViewConstraints(currentSpeedMutcdId, START, postedSpeedMutcdId, START),
        )
        updateMutcdConstraints(0, WRAP_CONTENT, mutcdConstraints)

        val currentSpeedViennaId = speedInfoCurrentSpeedVienna.id
        val postedSpeedViennaId = speedInfoPostedSpeedLayoutVienna.id
        val viennaConstraints = listOf(
            ViewConstraints(postedSpeedViennaId, END, PARENT_ID, END),
            ViewConstraints(postedSpeedViennaId, START, PARENT_ID, START),
            ViewConstraints(postedSpeedViennaId, BOTTOM, PARENT_ID, BOTTOM),
            ViewConstraints(viewId = postedSpeedViennaId, anchor = TOP, shouldConnect = false),
            ViewConstraints(currentSpeedViennaId, TOP, PARENT_ID, TOP),
            ViewConstraints(currentSpeedViennaId, END, postedSpeedViennaId, END),
            ViewConstraints(currentSpeedViennaId, BOTTOM, postedSpeedViennaId, TOP),
            ViewConstraints(currentSpeedViennaId, START, postedSpeedViennaId, START),
        )
        updateViennaConstraints(0, WRAP_CONTENT, viennaConstraints)
    }

    private fun renderCurrentSpeedToEnd() {
        val currentSpeedMutcdId = speedInfoCurrentSpeedMutcd.id
        val postedSpeedMutcdId = speedInfoPostedSpeedLayoutMutcd.id
        val mutcdConstraints = listOf(
            ViewConstraints(postedSpeedMutcdId, TOP, PARENT_ID, TOP),
            ViewConstraints(postedSpeedMutcdId, START, PARENT_ID, START),
            ViewConstraints(postedSpeedMutcdId, BOTTOM, PARENT_ID, BOTTOM),
            ViewConstraints(viewId = postedSpeedMutcdId, anchor = END, shouldConnect = false),
            ViewConstraints(currentSpeedMutcdId, END, PARENT_ID, END),
            ViewConstraints(currentSpeedMutcdId, TOP, postedSpeedMutcdId, TOP),
            ViewConstraints(currentSpeedMutcdId, START, postedSpeedMutcdId, END),
            ViewConstraints(currentSpeedMutcdId, BOTTOM, postedSpeedMutcdId, BOTTOM),
        )
        updateMutcdConstraints(WRAP_CONTENT, 0, mutcdConstraints)

        val currentSpeedViennaId = speedInfoCurrentSpeedVienna.id
        val postedSpeedViennaId = speedInfoPostedSpeedLayoutVienna.id
        val viennaConstraints = listOf(
            ViewConstraints(postedSpeedViennaId, TOP, PARENT_ID, TOP),
            ViewConstraints(postedSpeedViennaId, START, PARENT_ID, START),
            ViewConstraints(postedSpeedViennaId, BOTTOM, PARENT_ID, BOTTOM),
            ViewConstraints(viewId = postedSpeedViennaId, anchor = END, shouldConnect = false),
            ViewConstraints(currentSpeedViennaId, END, PARENT_ID, END),
            ViewConstraints(currentSpeedViennaId, TOP, postedSpeedViennaId, TOP),
            ViewConstraints(currentSpeedViennaId, START, postedSpeedViennaId, END),
            ViewConstraints(currentSpeedViennaId, BOTTOM, postedSpeedViennaId, BOTTOM),
        )
        updateViennaConstraints(WRAP_CONTENT, 0, viennaConstraints)
    }

    private fun renderCurrentSpeedToStart() {
        val currentSpeedMutcdId = speedInfoCurrentSpeedMutcd.id
        val postedSpeedMutcdId = speedInfoPostedSpeedLayoutMutcd.id
        val mutcdConstraints = listOf(
            ViewConstraints(postedSpeedMutcdId, TOP, PARENT_ID, TOP),
            ViewConstraints(postedSpeedMutcdId, END, PARENT_ID, END),
            ViewConstraints(postedSpeedMutcdId, BOTTOM, PARENT_ID, BOTTOM),
            ViewConstraints(viewId = postedSpeedMutcdId, anchor = START, shouldConnect = false),
            ViewConstraints(currentSpeedMutcdId, START, PARENT_ID, START),
            ViewConstraints(currentSpeedMutcdId, TOP, postedSpeedMutcdId, TOP),
            ViewConstraints(currentSpeedMutcdId, END, postedSpeedMutcdId, START),
            ViewConstraints(currentSpeedMutcdId, BOTTOM, postedSpeedMutcdId, BOTTOM),
        )
        updateMutcdConstraints(WRAP_CONTENT, 0, mutcdConstraints)

        val currentSpeedViennaId = speedInfoCurrentSpeedVienna.id
        val postedSpeedViennaId = speedInfoPostedSpeedLayoutVienna.id
        val viennaConstraints = listOf(
            ViewConstraints(postedSpeedViennaId, TOP, PARENT_ID, TOP),
            ViewConstraints(postedSpeedViennaId, END, PARENT_ID, END),
            ViewConstraints(postedSpeedViennaId, BOTTOM, PARENT_ID, BOTTOM),
            ViewConstraints(viewId = postedSpeedViennaId, anchor = START, shouldConnect = false),
            ViewConstraints(currentSpeedViennaId, START, PARENT_ID, START),
            ViewConstraints(currentSpeedViennaId, TOP, postedSpeedViennaId, TOP),
            ViewConstraints(currentSpeedViennaId, END, postedSpeedViennaId, START),
            ViewConstraints(currentSpeedViennaId, BOTTOM, postedSpeedViennaId, BOTTOM),
        )
        updateViennaConstraints(WRAP_CONTENT, 0, viennaConstraints)
    }

    private fun renderCurrentSpeedToBottom() {
        val currentSpeedMutcdId = speedInfoCurrentSpeedMutcd.id
        val postedSpeedMutcdId = speedInfoPostedSpeedLayoutMutcd.id
        val mutcdConstraints = listOf(
            ViewConstraints(postedSpeedMutcdId, TOP, PARENT_ID, TOP),
            ViewConstraints(postedSpeedMutcdId, END, PARENT_ID, END),
            ViewConstraints(postedSpeedMutcdId, START, PARENT_ID, START),
            ViewConstraints(viewId = postedSpeedMutcdId, anchor = BOTTOM, shouldConnect = false),
            ViewConstraints(currentSpeedMutcdId, BOTTOM, PARENT_ID, BOTTOM),
            ViewConstraints(currentSpeedMutcdId, END, postedSpeedMutcdId, END),
            ViewConstraints(currentSpeedMutcdId, TOP, postedSpeedMutcdId, BOTTOM),
            ViewConstraints(currentSpeedMutcdId, START, postedSpeedMutcdId, START),
        )
        updateMutcdConstraints(0, WRAP_CONTENT, mutcdConstraints)

        val currentSpeedViennaId = speedInfoCurrentSpeedVienna.id
        val postedSpeedViennaId = speedInfoPostedSpeedLayoutVienna.id
        val viennaConstraints = listOf(
            ViewConstraints(postedSpeedViennaId, TOP, PARENT_ID, TOP),
            ViewConstraints(postedSpeedViennaId, END, PARENT_ID, END),
            ViewConstraints(postedSpeedViennaId, START, PARENT_ID, START),
            ViewConstraints(viewId = postedSpeedViennaId, anchor = BOTTOM, shouldConnect = false),
            ViewConstraints(currentSpeedViennaId, BOTTOM, PARENT_ID, BOTTOM),
            ViewConstraints(currentSpeedViennaId, END, postedSpeedViennaId, END),
            ViewConstraints(currentSpeedViennaId, TOP, postedSpeedViennaId, BOTTOM),
            ViewConstraints(currentSpeedViennaId, START, postedSpeedViennaId, START),
        )
        updateViennaConstraints(0, WRAP_CONTENT, viennaConstraints)
    }

    private fun updateMutcdConstraints(
        currentSpeedWidth: Int,
        currentSpeedHeight: Int,
        constraints: List<ViewConstraints>,
    ) {
        val set = ConstraintSet()
        speedInfoCurrentSpeedMutcd.updateLayoutParams {
            width = currentSpeedWidth
            height = currentSpeedHeight
        }
        set.clone(speedInfoMutcdLayout)

        constraints.forEach { viewConstraint ->
            if (viewConstraint.shouldConnect) {
                set.connect(
                    viewConstraint.startId,
                    viewConstraint.startSide,
                    viewConstraint.endId,
                    viewConstraint.endSide
                )
            } else {
                set.clear(viewConstraint.viewId, viewConstraint.anchor)
            }
        }

        set.applyTo(speedInfoMutcdLayout)
    }

    private fun updateViennaConstraints(
        currentSpeedWidth: Int,
        currentSpeedHeight: Int,
        constraints: List<ViewConstraints>,
    ) {
        val set = ConstraintSet()
        speedInfoCurrentSpeedVienna.updateLayoutParams {
            width = currentSpeedWidth
            height = currentSpeedHeight
        }
        set.clone(speedInfoViennaLayout)

        constraints.forEach { viewConstraint ->
            if (viewConstraint.shouldConnect) {
                set.connect(
                    viewConstraint.startId,
                    viewConstraint.startSide,
                    viewConstraint.endId,
                    viewConstraint.endSide
                )
            } else {
                set.clear(viewConstraint.viewId, viewConstraint.anchor)
            }
        }

        set.applyTo(speedInfoViennaLayout)
    }
}

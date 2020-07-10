package com.mapbox.navigation.examples.performance

import android.os.Build
import android.view.FrameMetrics
import android.view.Window
import androidx.annotation.RequiresApi
import kotlin.math.max
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.N)
class FrameMetricsListener(
    val options: FrameMetricOptions
) : Window.OnFrameMetricsAvailableListener {

    private var frameMetricsReport = FrameMetricsReport(options)
    private var totalDurationWindow = TotalDurationWindow(options.windowSize)

    fun report(): FrameMetricsReport = frameMetricsReport.copy()
    fun totalDuration(): Float = this.totalDurationWindow.value()

    override fun onFrameMetricsAvailable(window: Window, frameMetrics: FrameMetrics, dropCountSinceLastInvocation: Int) {
        val frameMetricsCopy = FrameMetrics(frameMetrics)
        val totalDurationMs = frameMetricsCopy.nanosToMillis(FrameMetrics.TOTAL_DURATION)

        frameMetricsReport.totalFrames++
        frameMetricsReport.maxDuration = max(totalDurationMs, frameMetricsReport.maxDuration)
        Timber.i("frame metric available total duration: %.2fms dropCount: %d".format(totalDurationMs, dropCountSinceLastInvocation))

        totalDurationWindow.update(totalDurationMs.toFloat())
        if (totalDurationMs > options.warningLevelMs) {
            val jankMessage = "Jank detected total duration: %.2fms\n".format(totalDurationMs) +
                "${mapToFrameMetricsJank(frameMetricsCopy)}"
            if (totalDurationMs > options.errorLevelMs) {
                frameMetricsReport.errorFrames++
                Timber.e(jankMessage)
            } else {
                frameMetricsReport.warningFrames++
                Timber.w(jankMessage)
            }
        }
    }


    private fun mapToFrameMetricsJank(frameMetrics: FrameMetrics): FrameMetricsJank {
        return FrameMetricsJank(
            frameMetrics.nanosToMillis(FrameMetrics.UNKNOWN_DELAY_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.INPUT_HANDLING_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.ANIMATION_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.LAYOUT_MEASURE_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.DRAW_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.SYNC_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.COMMAND_ISSUE_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.SWAP_BUFFERS_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.TOTAL_DURATION)
        )
    }

    private fun FrameMetrics.nanosToMillis(metric: Int): Double = getMetric(metric) * 0.000001

    private class TotalDurationWindow(windowSize: Int) {
        private val valueWindow = FloatArray(windowSize)
        private var currentIndex = 0

        fun update(totalDurationMs: Float) {
            valueWindow[currentIndex] = totalDurationMs
            currentIndex = (currentIndex + 1) % valueWindow.size
        }
        fun value(): Float = valueWindow.max() ?: valueWindow.last()
    }
}


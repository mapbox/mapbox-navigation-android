package com.mapbox.navigation.examples.performance

import android.graphics.Color
import android.os.Build
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.robinhood.spark.SparkView
import com.robinhood.spark.animation.MorphSparkAnimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.N)
class FrameMetricsSparkUi(
    private val activity: AppCompatActivity
) {
    private val frameMetricsPerformance = FrameMetricsPerformance().observe(activity)

    fun attach(sparkView: SparkView) {
        if (activity.lifecycle.currentState == Lifecycle.State.DESTROYED)
            return

        val sparkAdapter = FrameMetricsSparkAdapter(frameMetricsPerformance.options)
        sparkView.adapter = sparkAdapter
        sparkView.sparkAnimator = MorphSparkAnimator()
        sparkView.yPoints

        activity.lifecycle.addObserver(object : LifecycleEventObserver {
            private val uiScope = CoroutineScope(Dispatchers.Main)

            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> uiScope.start(sparkView, sparkAdapter)
                    Lifecycle.Event.ON_DESTROY -> uiScope.coroutineContext.cancel()
                    else -> {
                        // intentionally empty
                    }
                }
            }
        })
    }

    fun attach(reportText: TextView) {
        activity.lifecycle.addObserver(object : LifecycleEventObserver {
            private val uiScope = CoroutineScope(Dispatchers.Main)

            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> uiScope.start(reportText)
                    Lifecycle.Event.ON_DESTROY -> uiScope.coroutineContext.cancel()
                    else -> {
                        // intentionally empty
                    }
                }
            }
        })
    }

    private fun CoroutineScope.start(reportText: TextView) = launch {
        while (isActive) {
            if (activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                val report = frameMetricsPerformance.listener()?.report()
                val largestJankValue =  "%.2fms".format(report?.maxDuration)
                reportText.text = """
                    Largest Jank: $largestJankValue
                    Total frames measured: ${report?.totalFrames}
                    - warning frames: ${report?.warningFrames}
                    - error frames: ${report?.errorFrames}
                    + good frames ${report?.totalFrames?.minus(report?.errorFrames)?.minus(report?.warningFrames)}
                """.trimIndent()
            }

            delay(1000)
        }
    }

    private fun CoroutineScope.start(sparkView: SparkView, sparkAdapter: FrameMetricsSparkAdapter) = launch {
        while (isActive) {
            val totalDuration = frameMetricsPerformance.listener()?.totalDuration()
            Timber.i("what is this $totalDuration")
            if (totalDuration != null) {
                sparkAdapter.addSample(totalDuration)
            } else {
                sparkAdapter.addEmptySample()
            }

            val maxDuration = frameMetricsPerformance.listener()?.report()?.maxDuration
            if (maxDuration != null) {
                val options = frameMetricsPerformance.options
                sparkView.lineColor = when {
                    maxDuration > options.errorLevelMs -> Color.RED
                    maxDuration > options.warningLevelMs -> Color.YELLOW
                    else -> Color.GREEN
                }
            }

            delay(1000)
        }
    }
}

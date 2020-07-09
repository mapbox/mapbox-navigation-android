package com.mapbox.navigation.examples.performance

import android.app.Activity
import android.os.Build
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

data class FrameMetricOptions(
    val warningLevelMs: Long,
    val errorLevelMs: Long
) {
    class Builder {
        private var warningLevelMs: Long = 20
        private var errorLevelMs: Long = 200

        fun warningLevelMs(warningLevelMs: Long) =
            apply { this.warningLevelMs = warningLevelMs }
        fun errorLevelMs(errorLevelMs: Long) =
            apply { this.errorLevelMs = errorLevelMs }

        fun build(): FrameMetricOptions {
            return FrameMetricOptions(
                warningLevelMs,
                errorLevelMs
            )
        }
    }
}

class FrameMetricsPerformance @JvmOverloads constructor (
    val options: FrameMetricOptions = FrameMetricOptions.Builder().build()
) {
    private var frameMetricsListener: FrameMetricsListener? = null

    fun start(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val listener = FrameMetricsListener(options)
            activity.window.addOnFrameMetricsAvailableListener(listener, Handler())
            frameMetricsListener = listener
        } else {
            Timber.w("FrameMetrics can work only with Android SDK 24 (Nougat) and higher")
        }
    }

    fun stop(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            frameMetricsListener?.let {
                activity.window.removeOnFrameMetricsAvailableListener(it)
            }
            val report = frameMetricsListener?.report()
            Timber.i("final report $report")
            frameMetricsListener = null
        }
    }

    fun observe(activity: AppCompatActivity) {
        activity.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_CREATE -> start(activity)
                    Lifecycle.Event.ON_DESTROY -> stop(activity)
                    else -> {
                        // intentionally empty
                    }
                }
            }
        })
    }
}

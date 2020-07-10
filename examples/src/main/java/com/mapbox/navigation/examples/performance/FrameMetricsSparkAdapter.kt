package com.mapbox.navigation.examples.performance

import android.graphics.RectF
import com.robinhood.spark.SparkAdapter
import timber.log.Timber

class FrameMetricsSparkAdapter(
    val options: FrameMetricOptions
) : SparkAdapter() {
    private val yData = FloatArray(options.circleBufferSize)

    var currentIndex = 0

    fun addSample(sample: Float) {
        if (currentIndex == yData.lastIndex) {
            rotateLeft()
        } else {
            currentIndex++
        }
        yData[currentIndex] = sample
        notifyDataSetChanged()
    }

    fun addEmptySample() {
        addSample(yData[currentIndex])
        notifyDataSetChanged()
    }

    private fun rotateLeft() {
        for (i in 1..yData.lastIndex) {
            yData[i-1] = yData[i]
        }
    }

    override fun getCount(): Int {
        Timber.i("what is this get count ${yData.size}")
        return yData.size
    }

    override fun getItem(index: Int): Any {
        return yData[index]
    }

    override fun getY(index: Int): Float {
        return yData[index]
    }

    override fun getDataBounds(): RectF {
        val count = count
        val minY = 0.0f
        val maxY = options.errorLevelMs.toFloat()
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        for (i in 0 until count) {
            val x = getX(i)
            minX = minX.coerceAtMost(x)
            maxX = maxX.coerceAtLeast(x)
        }

        return RectF(minX, minY, maxX, maxY)
    }
}
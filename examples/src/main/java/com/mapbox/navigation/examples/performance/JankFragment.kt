package com.mapbox.navigation.examples.performance

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mapbox.navigation.examples.R
import com.robinhood.spark.SparkView

class JankFragment : Fragment() {

    private var frameMetricsSparkUi: FrameMetricsSparkUi? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.performance_jank_fragment, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            frameMetricsSparkUi = FrameMetricsSparkUi(context as AppCompatActivity)
        }
    }

    override fun onDetach() {
        frameMetricsSparkUi = null
        super.onDetach()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sparkView = view.findViewById<SparkView>(R.id.sparkView)
            val reportText = view.findViewById<TextView>(R.id.reportText)
            val scrubText = view.findViewById<TextView>(R.id.scrubText)
            frameMetricsSparkUi?.attach(sparkView)
            frameMetricsSparkUi?.attach(reportText)
            sparkView.scrubListener = SparkView.OnScrubListener {
                val timeMessage = when (it) {
                    is Float -> "%.2fms".format(it)
                    else -> null
                }
                scrubText.text = timeMessage ?: ""
                scrubText.visibility = if (timeMessage != null) View.VISIBLE else View.GONE
            }
        }
    }
}
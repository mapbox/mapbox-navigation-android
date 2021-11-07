package com.mapbox.navigation.examples

import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.core.databinding.TestActivityBinding
import com.mapbox.navigation.ui.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.ui.shield.view.MapboxRouteShieldView

class TestActivity : AppCompatActivity() {

    private lateinit var binding: TestActivityBinding
    val mapboxRouteShieldApi = MapboxRouteShieldApi(
        accessToken = "pk.eyJ1IjoiZHJpdmVyYXBwIiwiYSI6ImNrdW82Nmx5OTBiZTAydW1kaDRkcG90ODUifQ.QsFw2CPkp543gDRTQlz8mQ"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TestActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*val shield = mapboxRouteShieldApi.getData()

        binding.routeShieldView.render(shield)*/
    }

    fun pixelsToSp(px: Float): Float {
        val scaledDensity: Float = resources.displayMetrics.scaledDensity
        return px / scaledDensity
    }

    fun convertPixelsToDp(px: Float): Float {
        return px / (resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
    }

    /*private fun test() {
        Log.d("abhishek_testing", "imageview height: ${convertPixelsToDp(81f)}; imageview width: ${convertPixelsToDp(72f)}")
        val height = convertPixelsToDp(((81 / 27) * 16.5).toFloat())
        "width": 60,
"height": 42,
"x": 552,
"y": 963,
"pixelRatio": 1,
"placeholder": [
0,
4,
20,
10
],
"visible": true
        Log.d("abhishek_testing", "placeholder height: $height")
        val parent = findViewById<ConstraintLayout>(R.id.mainContent)
        val shield = parent.getChildAt(0)
        val set = ConstraintSet()

        val placeholder = TextView(this)
        placeholder.id = View.generateViewId()
        // placeholder.setBackgroundColor(Color.BLACK)
        placeholder.text = "880"
        placeholder.gravity = Gravity.CENTER
        placeholder.setTextColor(Color.WHITE)
        placeholder.includeFontPadding = false
        placeholder.textSize = pixelsToSp(height)
        parent.addView(placeholder, 1)
        val params = ConstraintLayout.LayoutParams(0, height.toInt())
        placeholder.layoutParams = params

        set.clone(parent)
        set.connect(placeholder.id, ConstraintSet.TOP, shield.id, ConstraintSet.TOP)
        set.connect(placeholder.id, ConstraintSet.END, shield.id, ConstraintSet.END)
        set.connect(placeholder.id, ConstraintSet.START, shield.id, ConstraintSet.START)
        set.connect(placeholder.id, ConstraintSet.BOTTOM, shield.id, ConstraintSet.BOTTOM)
        set.applyTo(parent)
    }*/
}

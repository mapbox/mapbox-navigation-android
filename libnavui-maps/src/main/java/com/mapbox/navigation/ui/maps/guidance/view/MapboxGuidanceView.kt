package com.mapbox.navigation.ui.maps.guidance.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.guidanceimage.GuidanceImageState
import com.mapbox.navigation.ui.maps.R
import kotlinx.android.synthetic.main.layout_guidance_view.view.*

class MapboxGuidanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): MapboxView<GuidanceImageState>, CardView(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_guidance_view, this, true)
        visibility = GONE
    }

    override fun render(state: GuidanceImageState) {
        when (state) {
            is GuidanceImageState.GuidanceImagePrepared -> {
                visibility = VISIBLE
                mapboxGuidanceView.setImageBitmap(state.bitmap)
            }
            is GuidanceImageState.GuidanceImageFailure.GuidanceImageUnavailable -> {
                mapboxGuidanceView.setImageBitmap(null)
                visibility = GONE
            }
            is GuidanceImageState.GuidanceImageFailure.GuidanceImageEmpty -> {
                mapboxGuidanceView.setImageBitmap(null)
                visibility = GONE
            }
            is GuidanceImageState.GuidanceImageFailure.GuidanceImageError -> {
                mapboxGuidanceView.setImageBitmap(null)
                visibility = GONE
            }
        }
    }
}

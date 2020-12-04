package com.mapbox.navigation.ui.maps.guidance.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.guidanceimage.GuidanceImageState

class MapboxGuidanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MapboxView<GuidanceImageState>, AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        visibility = GONE
    }

    override fun render(state: GuidanceImageState) {
        when (state) {
            is GuidanceImageState.GuidanceImagePrepared -> {
                visibility = VISIBLE
                setImageBitmap(state.bitmap)
            }
            is GuidanceImageState.GuidanceImageFailure.GuidanceImageUnavailable -> {
                setImageBitmap(null)
                visibility = GONE
            }
            is GuidanceImageState.GuidanceImageFailure.GuidanceImageEmpty -> {
                setImageBitmap(null)
                visibility = GONE
            }
            is GuidanceImageState.GuidanceImageFailure.GuidanceImageError -> {
                setImageBitmap(null)
                visibility = GONE
            }
        }
    }
}

package com.mapbox.navigation.ui.maps.signboard.view

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.signboard.SignboardState

class MapboxSignboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MapboxView<SignboardState>, AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        visibility = GONE
    }

    override fun render(state: SignboardState) {
        when (state) {
            is SignboardState.SignboardReady -> {
                //val bitmap = BitmapFactory.decodeStream(state.bytes)
                visibility = VISIBLE
                //setImageBitmap(bitmap)
            }
            is SignboardState.SignboardFailure.SignboardUnavailable -> {
                visibility = GONE
                setImageBitmap(null)
            }
            is SignboardState.SignboardFailure.SignboardError -> {
                visibility = GONE
                setImageBitmap(null)
            }
        }
    }
}

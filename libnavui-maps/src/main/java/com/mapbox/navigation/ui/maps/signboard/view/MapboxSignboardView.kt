package com.mapbox.navigation.ui.maps.signboard.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.signboard.SignboardState

/**
 * Default Signboard View that renders snapshot based on [SignboardState]
 */
class MapboxSignboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MapboxView<SignboardState>, AppCompatImageView(context, attrs, defStyleAttr) {

    /**
     * Initialize method call
     */
    init {
        visibility = GONE
    }

    /**
     * Entry point for the [MapboxSignboardView] to render itself based on a [SignboardState].
     */
    override fun render(state: SignboardState) {
        when (state) {
            is SignboardState.SignboardReady -> {
                visibility = VISIBLE
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

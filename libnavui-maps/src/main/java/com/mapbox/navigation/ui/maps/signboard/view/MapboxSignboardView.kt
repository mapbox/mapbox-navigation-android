package com.mapbox.navigation.ui.maps.signboard.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.signboard.SignboardState
import java.io.ByteArrayInputStream

/**
 * Default Signboard View that renders snapshot based on [SignboardState]
 */
class MapboxSignboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MapboxView<SignboardState>, AppCompatImageView(context, attrs, defStyleAttr) {

    private companion object {
        /**
         * default css rules for signboard text
         */
        private const val CSS_RULES =
            "text { font-family: Arial, Helvetica, sans-serif; font-size: 0.8em}"
    }

    /**
     * Entry point for the [MapboxSignboardView] to render itself based on a [SignboardState].
     */
    override fun render(state: SignboardState) {
        when (state) {
            is SignboardState.Signboard.Available -> {
                val signboard = renderSignboard(state)
                setImageBitmap(signboard)
            }
            is SignboardState.Show -> {
                visibility = VISIBLE
            }
            is SignboardState.Hide -> {
                visibility = GONE
            }
            is SignboardState.Signboard.Empty,
            is SignboardState.Signboard.Error -> {
                setImageBitmap(null)
            }
            else -> {
            }
        }
    }

    private fun renderSignboard(state: SignboardState.Signboard.Available): Bitmap? {
        return when (state.bytes.isEmpty()) {
            true -> null
            else -> {
                val stream = ByteArrayInputStream(state.bytes)
                val svg = SVG.getFromInputStream(stream)

                val aspectRatio = svg.documentViewBox.bottom / svg.documentViewBox.right
                val calculatedHeight = (state.desiredSignboardWidth * aspectRatio).toInt()

                val signboard = Bitmap.createBitmap(
                    state.desiredSignboardWidth,
                    calculatedHeight,
                    Bitmap.Config.ARGB_8888
                )
                val renderOptions = RenderOptions.create()
                renderOptions.css(CSS_RULES)
                val signboardCanvas = Canvas(signboard)
                svg.renderToCanvas(signboardCanvas, renderOptions)
                signboard
            }
        }
    }
}

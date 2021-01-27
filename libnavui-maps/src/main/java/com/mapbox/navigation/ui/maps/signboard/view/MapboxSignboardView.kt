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
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import java.io.ByteArrayInputStream

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
                ifNonNull(renderSignboard(state.bytes)) { bitmap ->
                    visibility = VISIBLE
                    setImageBitmap(bitmap)
                }
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

    private fun renderSignboard(data: ByteArray): Bitmap? {
        val stream = ByteArrayInputStream(data)
        val svg = SVG.getFromInputStream(stream)

        val aspectRatio = svg.documentViewBox.bottom / svg.documentViewBox.right
        val definedWidth = 300
        val calculatedHeight = (definedWidth * aspectRatio).toInt()

        val signboard = Bitmap.createBitmap(
            definedWidth,
            calculatedHeight,
            Bitmap.Config.ARGB_8888
        )
        val renderOptions = RenderOptions.create()
        renderOptions.css("text { font-family: Arial, Helvetica, sans-serif; }")
        val signboardCanvas = Canvas(signboard)
        svg.renderToCanvas(signboardCanvas, renderOptions)
        return signboard
    }
}

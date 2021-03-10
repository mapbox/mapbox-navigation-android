package com.mapbox.navigation.ui.utils.internal

import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import java.io.ByteArrayInputStream

object SvgUtil {

    private const val CSS_RULES =
        "text { font-family: Arial, Helvetica, sans-serif; font-size: 0.8em}"

    @JvmStatic
    fun renderAsBitmapWithHeight(stream: ByteArrayInputStream, desiredHeight: Int): Bitmap? {
        try {
            val svg = SVG.getFromInputStream(stream)

            svg.setDocumentWidth("100%")
            svg.setDocumentHeight("100%")
            return if (svg.documentViewBox == null) {
                null
            } else {
                val aspectRatio = svg.documentViewBox.bottom / svg.documentViewBox.right
                val calculatedWidth = (desiredHeight / aspectRatio).toInt()

                val signboard = Bitmap.createBitmap(
                    calculatedWidth,
                    desiredHeight,
                    Bitmap.Config.ARGB_8888
                )
                val signboardCanvas = Canvas(signboard)
                svg.renderToCanvas(signboardCanvas)
                signboard
            }
        } catch (exception: Exception) {
            return null
        }
    }

    @JvmStatic
    fun renderAsBitmapWithWidth(stream: ByteArrayInputStream, desiredWidth: Int): Bitmap? {
        try {
            val svg = SVG.getFromInputStream(stream)

            svg.setDocumentWidth("100%")
            svg.setDocumentHeight("100%")
            return if (svg.documentViewBox == null) {
                null
            } else {
                val aspectRatio = svg.documentViewBox.bottom / svg.documentViewBox.right
                val calculatedHeight = (desiredWidth * aspectRatio).toInt()

                val signboard = Bitmap.createBitmap(
                    desiredWidth,
                    calculatedHeight,
                    Bitmap.Config.ARGB_8888
                )
                val renderOptions = RenderOptions.create()
                renderOptions.css(CSS_RULES)
                val signboardCanvas = Canvas(signboard)
                svg.renderToCanvas(signboardCanvas, renderOptions)
                signboard
            }
        } catch (exception: Exception) {
            return null
        }
    }
}

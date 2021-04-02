package com.mapbox.navigation.ui.utils.internal

import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import java.io.ByteArrayInputStream

object SvgUtil {

    @JvmStatic
    fun renderAsBitmapWithHeight(
        stream: ByteArrayInputStream,
        desiredHeight: Int,
        cssStyles: String? = null
    ): Bitmap? {
        return try {
            val svg = SVG.getFromInputStream(stream)

            svg.setDocumentWidth("100%")
            svg.setDocumentHeight("100%")
            if (svg.documentViewBox == null) {
                null
            } else {
                val aspectRatio = svg.documentViewBox.bottom / svg.documentViewBox.right
                val calculatedWidth = (desiredHeight / aspectRatio).toInt()
                renderBitmap(svg, calculatedWidth, desiredHeight, cssStyles)
            }
        } catch (exception: Exception) {
            null
        }
    }

    @JvmStatic
    suspend fun renderAsBitmapWithWidth(
        stream: ByteArrayInputStream,
        desiredWidth: Int,
        cssStyles: String? = null
    ): Bitmap? {
        return try {
            val svg = SVG.getFromInputStream(stream)

            svg.setDocumentWidth("100%")
            svg.setDocumentHeight("100%")
            if (svg.documentViewBox == null) {
                null
            } else {
                val aspectRatio = svg.documentViewBox.bottom / svg.documentViewBox.right
                val calculatedHeight = (desiredWidth * aspectRatio).toInt()
                renderBitmap(svg, desiredWidth, calculatedHeight, cssStyles)
            }
        } catch (exception: Exception) {
            null
        }
    }

    private fun renderBitmap(
        svg: SVG,
        width: Int,
        height: Int,
        cssStyles: String? = null
    ): Bitmap? {
        val signboard = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val signboardCanvas = Canvas(signboard)
        cssStyles?.let {
            val renderOptions = RenderOptions.create()
            renderOptions.css(it)
            svg.renderToCanvas(signboardCanvas, renderOptions)
        } ?: svg.renderToCanvas(signboardCanvas)
        return signboard
    }
}

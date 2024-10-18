package com.mapbox.navigation.ui.utils.internal

import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGExternalFileResolver
import com.caverock.androidsvg.SVGParseException
import com.mapbox.navigation.utils.internal.logE
import java.io.InputStream

object SvgUtil {

    private const val LOG_CATEGORY = "SvgUtil"

    fun renderAsBitmapWith(
        stream: InputStream,
        desiredWidth: Int,
        desiredHeight: Int,
        cssStyles: String? = null,
    ): Bitmap? {
        return try {
            val svg = SVG.getFromInputStream(stream)

            svg.setDocumentWidth("100%")
            svg.setDocumentHeight("100%")
            if (svg.documentViewBox == null) {
                null
            } else {
                renderBitmap(svg, desiredWidth, desiredHeight, cssStyles)
            }
        } catch (exception: SVGParseException) {
            logE("${exception.message}", LOG_CATEGORY)
            null
        } catch (exception: IllegalArgumentException) {
            logE("${exception.message}", LOG_CATEGORY)
            null
        }
    }

    fun renderAsBitmapWithHeight(
        stream: InputStream,
        desiredHeight: Int,
        cssStyles: String? = null,
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
        } catch (exception: SVGParseException) {
            logE("${exception.message}", LOG_CATEGORY)
            null
        } catch (exception: IllegalArgumentException) {
            logE("${exception.message}", LOG_CATEGORY)
            null
        }
    }

    @Throws
    fun renderAsBitmapWithWidth(
        stream: InputStream,
        desiredWidth: Int,
        cssStyles: String? = null,
        resolver: SVGExternalFileResolver? = null,
    ): Bitmap {
        ifNonNull(resolver) { svgResolver ->
            SVG.registerExternalFileResolver(svgResolver)
        }
        val svg = SVG.getFromInputStream(stream)
        svg.setDocumentWidth("100%")
        svg.setDocumentHeight("100%")

        return if (svg.documentViewBox == null) {
            throw Exception("SVG's viewBox is null")
        } else {
            val aspectRatio = svg.documentViewBox.bottom / svg.documentViewBox.right
            val calculatedHeight = (desiredWidth * aspectRatio).toInt()
            renderBitmap(svg, desiredWidth, calculatedHeight, cssStyles)
        }
    }

    private fun renderBitmap(
        svg: SVG,
        width: Int,
        height: Int,
        cssStyles: String? = null,
    ): Bitmap {
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

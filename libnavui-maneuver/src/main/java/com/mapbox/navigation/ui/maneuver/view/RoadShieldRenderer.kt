package com.mapbox.navigation.ui.maneuver.view

import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.SVG
import java.io.ByteArrayInputStream

internal object RoadShieldRenderer {

    fun renderRoadShieldAsBitmap(byteArray: ByteArray, desiredHeight: Int): Bitmap? {
        val stream = ByteArrayInputStream(byteArray)
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
    }
}

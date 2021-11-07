package com.mapbox.navigation.ui.shield.api

import android.content.res.Resources
import android.graphics.Bitmap
import android.util.TypedValue
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import java.io.ByteArrayInputStream

private const val DEFAULT_HEIGHT_FOR_LEGACY_DIP = 36f

/**
 * Invoke the method to convert [RouteShield.MapboxDesignedShield.byteArray] to [Bitmap].
 *
 * @param desiredHeight desired height of the bitmap in pixel. Width is calculated automatically to
 * maintain the aspect ratio. If not specified, height and width are obtained from
 * [RouteShield.MapboxDesignedShield.shieldSprite] associated with the shield.
 */
fun RouteShield.MapboxDesignedShield.toBitmap(
    resources: Resources,
    desiredHeight: Int? = null
): Bitmap? {
    return if (desiredHeight != null) {
        val stream = ByteArrayInputStream(byteArray)
        SvgUtil.renderAsBitmapWithHeight(stream, desiredHeight)
    } else {
        val spriteWidth =
            shieldSprite.spriteAttributes().width().toFloat()
        val spriteHeight =
            shieldSprite.spriteAttributes().height().toFloat()
        val widthPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            spriteWidth,
            resources.displayMetrics
        ).toInt()
        val heightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            spriteHeight,
            resources.displayMetrics
        ).toInt()
        val stream = ByteArrayInputStream(byteArray)
        SvgUtil.renderAsBitmapWith(stream, widthPx, heightPx)
    }
}

/**
 * Invoke the method to convert [RouteShield.MapboxLegacyShield.byteArray] to [Bitmap].
 *
 * @param desiredHeight desired height of the bitmap in pixel. Width is calculated automatically to
 * maintain the aspect ratio. If not specified, height is defaulted to 100 pixel
 */
fun RouteShield.MapboxLegacyShield.toBitmap(
    resources: Resources,
    desiredHeight: Int? = null
): Bitmap? {
    val heightPx = desiredHeight
        ?: TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            DEFAULT_HEIGHT_FOR_LEGACY_DIP,
            resources.displayMetrics
        ).toInt()
    val stream = ByteArrayInputStream(byteArray)
    return SvgUtil.renderAsBitmapWithHeight(stream, heightPx)
}

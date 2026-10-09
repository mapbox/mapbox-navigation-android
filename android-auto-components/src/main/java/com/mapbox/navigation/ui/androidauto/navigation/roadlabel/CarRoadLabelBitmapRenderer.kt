package com.mapbox.navigation.ui.androidauto.navigation.roadlabel

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.ui.androidauto.internal.RendererUtils.dpScale
import kotlin.math.roundToInt

/**
 * This class will a road name and create a bitmap that fits the text.
 */
internal class CarRoadLabelBitmapRenderer {

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cachedDimens: Dimens? = null

    // The density does not change for a surface, so the sizes are computed once.
    private fun dimens(scale: Float): Dimens =
        cachedDimens?.takeIf { it.scale == scale } ?: Dimens(scale).also { cachedDimens = it }

    /**
     * Render [road] and [shields] to a [Bitmap]
     */
    fun render(
        resources: Resources,
        road: List<RoadComponent>,
        shields: List<RouteShield>,
        options: CarRoadLabelOptions = CarRoadLabelOptions.default,
    ): Bitmap? {
        if (road.isEmpty()) return null
        val dimens = dimens(resources.dpScale())
        textPaint.color = options.textColor
        textPaint.textSize = dimens.textSize.toFloat()
        val components = measureRoadLabel(resources, road, shields, dimens.textSize)
        val spaceWidth = textPaint.measureText(" ").roundToInt()
        val width = components.sumOf { component ->
            when (component) {
                is Component.Text -> component.rect.width()
                is Component.Shield -> component.bitmap.width
            }
        } + spaceWidth * components.lastIndex
        val height = components.maxOf { component ->
            when (component) {
                is Component.Text -> component.rect.height()
                is Component.Shield -> component.bitmap.height
            }
        }
        val bitmap = Bitmap.createBitmap(
            width + dimens.textPadding * 2,
            height + dimens.textPadding * 2,
            Bitmap.Config.ARGB_8888,
        )
        val textBaselineY =
            dimens.textPadding + (height - textPaint.descent() - textPaint.ascent()) / 2
        val shieldCenterY = dimens.textPadding + height / 2f
        bitmap.eraseColor(options.backgroundColor)
        Canvas(bitmap)
            .drawLabelBackground(options, dimens)
            .drawRoadLabel(components, textBaselineY, shieldCenterY, spaceWidth, dimens)

        return bitmap
    }

    private fun measureRoadLabel(
        resources: Resources,
        road: List<RoadComponent>,
        shields: List<RouteShield>,
        shieldHeight: Int,
    ): List<Component> {
        return road.map { component ->
            getShieldBitmap(resources, component, shields, shieldHeight)
                ?.let { Component.Shield(it) }
                ?: Component.Text(component.text, getTextBounds(component.text))
        }
    }

    private fun getShieldBitmap(
        resources: Resources,
        component: RoadComponent,
        shields: List<RouteShield>,
        shieldHeight: Int,
    ): Bitmap? {
        val shield = component.shield?.let { shield ->
            shields.find { it is RouteShield.MapboxDesignedShield && it.compareWith(shield) }
        } ?: component.imageBaseUrl?.let { baseUrl ->
            shields.find { it is RouteShield.MapboxLegacyShield && it.compareWith(baseUrl) }
        } ?: return null
        return shield.toBitmap(resources, shieldHeight)
    }

    private fun getTextBounds(text: String): Rect {
        return Rect().also { textPaint.getTextBounds(text, 0, text.length, it) }
    }

    private fun Canvas.drawLabelBackground(
        options: CarRoadLabelOptions,
        dimens: Dimens,
    ) = apply {
        val cardWidth = width - dimens.labelPadding
        val cardHeight = height - dimens.labelPadding

        labelPaint.color = options.roundedLabelColor
        if (options.shadowColor == null) {
            labelPaint.clearShadowLayer()
        } else {
            labelPaint.setShadowLayer(
                dimens.labelHeight,
                0f,
                dimens.labelHeight,
                options.shadowColor,
            )
        }

        drawRoundRect(
            dimens.labelPadding,
            dimens.labelPadding,
            cardWidth,
            cardHeight,
            dimens.labelRadius,
            dimens.labelRadius,
            labelPaint,
        )
    }

    private fun Canvas.drawRoadLabel(
        components: List<Component>,
        textBaselineY: Float,
        shieldCenterY: Float,
        spaceWidth: Int,
        dimens: Dimens,
    ) = apply {
        components.fold(dimens.textPadding) { x, component ->
            x + spaceWidth + when (component) {
                is Component.Text -> {
                    drawText(
                        component.value,
                        x + component.rect.width() / 2f,
                        textBaselineY,
                        textPaint,
                    )
                    component.rect.width()
                }

                is Component.Shield -> {
                    val shieldY = shieldCenterY - component.bitmap.height / 2f
                    drawBitmap(component.bitmap, x.toFloat(), shieldY, null)
                    component.bitmap.width
                }
            }
        }
    }

    private sealed class Component {
        data class Text(val value: String, val rect: Rect) : Component()
        data class Shield(val bitmap: Bitmap) : Component()
    }

    /**
     * The label sizes in pixels. The label is drawn on the map surface, so the sizes follow the
     * head unit density.
     */
    private class Dimens(val scale: Float) {
        val textSize = (TEXT_SIZE_DP * scale).roundToInt()
        val textPadding = (TEXT_PADDING_DP * scale).roundToInt()
        val labelPadding = LABEL_PADDING_DP * scale
        val labelRadius = LABEL_RADIUS_DP * scale
        val labelHeight = LABEL_HEIGHT_DP * scale
    }

    private companion object {
        private const val TEXT_SIZE_DP = 18
        private const val TEXT_PADDING_DP = 20

        private const val LABEL_PADDING_DP = 10f
        private const val LABEL_RADIUS_DP = 16f
        private const val LABEL_HEIGHT_DP = 3f
    }
}

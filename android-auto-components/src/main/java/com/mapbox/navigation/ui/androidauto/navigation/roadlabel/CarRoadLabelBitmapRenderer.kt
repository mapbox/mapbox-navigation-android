package com.mapbox.navigation.ui.androidauto.navigation.roadlabel

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import kotlin.math.roundToInt

/**
 * This class will a road name and create a bitmap that fits the text.
 */
internal class CarRoadLabelBitmapRenderer {

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
        textPaint.color = options.textColor
        val components = measureRoadLabel(resources, road, shields)
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
            width + TEXT_PADDING * 2,
            height + TEXT_PADDING * 2,
            Bitmap.Config.ARGB_8888,
        )
        val textBaselineY = TEXT_PADDING + (height - textPaint.descent() - textPaint.ascent()) / 2
        val shieldCenterY = TEXT_PADDING + height / 2f
        bitmap.eraseColor(options.backgroundColor)
        Canvas(bitmap)
            .drawLabelBackground(options)
            .drawRoadLabel(components, textBaselineY, shieldCenterY, spaceWidth)

        return bitmap
    }

    private fun measureRoadLabel(
        resources: Resources,
        road: List<RoadComponent>,
        shields: List<RouteShield>,
    ): List<Component> {
        return road.map { component ->
            getShieldBitmap(resources, component, shields)
                ?.let { Component.Shield(it) }
                ?: Component.Text(component.text, getTextBounds(component.text))
        }
    }

    private fun getShieldBitmap(
        resources: Resources,
        component: RoadComponent,
        shields: List<RouteShield>,
    ): Bitmap? {
        val shield = component.shield?.let { shield ->
            shields.find { it is RouteShield.MapboxDesignedShield && it.compareWith(shield) }
        } ?: component.imageBaseUrl?.let { baseUrl ->
            shields.find { it is RouteShield.MapboxLegacyShield && it.compareWith(baseUrl) }
        } ?: return null
        return shield.toBitmap(resources, TEXT_SIZE)
    }

    private fun getTextBounds(text: String): Rect {
        return Rect().also { textPaint.getTextBounds(text, 0, text.length, it) }
    }

    private fun Canvas.drawLabelBackground(options: CarRoadLabelOptions) = apply {
        val cardWidth = width - LABEL_PADDING
        val cardHeight = height - LABEL_PADDING

        labelPaint.color = options.roundedLabelColor
        if (options.shadowColor == null) {
            labelPaint.clearShadowLayer()
        } else {
            labelPaint.setShadowLayer(LABEL_HEIGHT, 0f, LABEL_HEIGHT, options.shadowColor)
        }

        drawRoundRect(
            LABEL_PADDING,
            LABEL_PADDING,
            cardWidth,
            cardHeight,
            LABEL_RADIUS,
            LABEL_RADIUS,
            labelPaint,
        )
    }

    private fun Canvas.drawRoadLabel(
        components: List<Component>,
        textBaselineY: Float,
        shieldCenterY: Float,
        spaceWidth: Int,
    ) = apply {
        components.fold(TEXT_PADDING) { x, component ->
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

    private companion object {
        private const val TEXT_SIZE = 18
        private const val TEXT_PADDING = 20

        private const val LABEL_PADDING = 10f
        private const val LABEL_RADIUS = 16f
        private const val LABEL_HEIGHT = 3f

        private val textPaint by lazy {
            Paint().apply {
                textSize = TEXT_SIZE.toFloat()
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
        }

        private val labelPaint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }
    }
}

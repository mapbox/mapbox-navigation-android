package com.mapbox.navigation.ui.androidauto.navigation.roadlabel

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.createBitmap
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
     * Render [road] and [shields] to a [Bitmap] that is at most [maxWidthPx] wide. The components
     * that don't fit are dropped from the end, and the last text that fits partly is ellipsized.
     */
    fun render(
        resources: Resources,
        road: List<RoadComponent>,
        shields: List<RouteShield>,
        options: CarRoadLabelOptions = CarRoadLabelOptions.default,
        maxWidthPx: Int = Int.MAX_VALUE,
    ): Bitmap? {
        if (road.isEmpty()) return null
        val dimens = dimens(resources.dpScale())
        textPaint.color = options.textColor
        textPaint.textSize = dimens.textSize.toFloat()
        val spaceWidth = textPaint.measureText(" ").roundToInt()
        val components = fitToWidth(
            measureRoadLabel(resources, road, shields, dimens.textSize),
            maxWidthPx - dimens.textPadding * 2,
            spaceWidth,
        )
        if (components.isEmpty()) return null
        val width = components.sumOf { it.width } + spaceWidth * components.lastIndex
        val height = components.maxOf { it.height }
        val bitmap = createBitmap(width + dimens.textPadding * 2, height + dimens.textPadding * 2)
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

    private fun fitToWidth(
        components: List<Component>,
        maxWidth: Int,
        spaceWidth: Int,
    ): List<Component> {
        val fitted = mutableListOf<Component>()
        var usedWidth = 0
        for (component in components) {
            val start = if (fitted.isEmpty()) 0 else usedWidth + spaceWidth
            val available = maxWidth - start
            if (component.width <= available) {
                fitted += component
                usedWidth = start + component.width
                continue
            }
            if (component is Component.Text) {
                ellipsize(component.value, available)?.let { fitted += it }
            }
            break
        }
        return fitted
    }

    /**
     * The longest prefix of [text] followed by an ellipsis that fits in [maxWidth], measured the
     * same way as the other components. Null when not even one character fits.
     */
    private fun ellipsize(text: String, maxWidth: Int): Component.Text? {
        var fitted: Component.Text? = null
        var low = 1
        var high = text.length - 1
        while (low <= high) {
            val length = (low + high) / 2
            val prefix = text.takeWholeChars(length).trimEnd()
            val candidate = prefix + ELLIPSIS
            val bounds = getTextBounds(candidate)
            if (bounds.width() <= maxWidth) {
                // A lone ellipsis says nothing about the road, so it is never used.
                if (prefix.isNotEmpty()) fitted = Component.Text(candidate, bounds)
                low = length + 1
            } else {
                high = length - 1
            }
        }
        return fitted
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
                        x + component.width / 2f,
                        textBaselineY,
                        textPaint,
                    )
                    component.width
                }

                is Component.Shield -> {
                    val shieldY = shieldCenterY - component.height / 2f
                    drawBitmap(component.bitmap, x.toFloat(), shieldY, null)
                    component.width
                }
            }
        }
    }

    private sealed class Component {
        abstract val width: Int
        abstract val height: Int

        data class Text(val value: String, val rect: Rect) : Component() {
            override val width get() = rect.width()
            override val height get() = rect.height()
        }

        data class Shield(val bitmap: Bitmap) : Component() {
            override val width get() = bitmap.width
            override val height get() = bitmap.height
        }
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

        private const val ELLIPSIS = "\u2026"
    }
}

/**
 * The first [length] chars, without the high half of a surrogate pair at the end. Cutting the
 * pair would leave a broken glyph before the ellipsis.
 */
@VisibleForTesting
internal fun String.takeWholeChars(length: Int): String {
    val prefix = take(length)
    return if (prefix.lastOrNull()?.isHighSurrogate() == true) prefix.dropLast(1) else prefix
}

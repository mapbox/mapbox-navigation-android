package com.mapbox.navigation.ui.maps.roadname.view

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.shield.model.RouteShieldError
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import com.mapbox.navigation.ui.utils.internal.extensions.drawableWithHeight
import com.mapbox.navigation.ui.utils.internal.ifNonNull

/**
 * Default Mapbox implementation that allows you to render road name labels and route shields
 * associated with the name.
 */
class MapboxRoadNameView : AppCompatTextView {

    private val shields = mutableSetOf<RouteShield>()
    private val roadComponents = mutableListOf<RoadComponent>()

    /**
     * Default view to render a road name label.
     */
    constructor(context: Context) : super(context)

    /**
     * Default view to render a road name label.
     */
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     * Default view to render a road name label.
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    /**
     * Invoke the method to render the view with road name.
     * It's recommended to hide this view if [Road] has no data to show.
     *
     * @param road Road
     */
    fun renderRoadName(road: Road) {
        roadComponents.clear()
        roadComponents.addAll(road.components)
        renderRoadNameLabel()
    }

    /**
     * Invoke the method to render the view with route shields.
     *
     * @param expectedShields list of shields to render
     */
    fun renderRoadNameWith(expectedShields: List<Expected<RouteShieldError, RouteShieldResult>>) {
        expectedShields.forEach { expected ->
            expected.onValue {
                this.shields.add(it.shield)
            }
        }
        renderRoadNameLabel()
    }

    private fun renderRoadNameLabel() {
        val roadNameLabel = SpannableStringBuilder()
        roadComponents.forEach { component ->
            val routeShield = when {
                component.shield != null || component.imageBaseUrl != null -> {
                    val routeShield = shields.find {
                        when (it) {
                            is RouteShield.MapboxDesignedShield -> {
                                it.compareWith(other = component.shield)
                            }
                            is RouteShield.MapboxLegacyShield -> {
                                it.compareWith(other = component.imageBaseUrl)
                            }
                            else -> {
                                false
                            }
                        }
                    }
                    routeShield
                }
                else -> {
                    null
                }
            }
            roadNameLabel.renderRoadName(
                text = component.text,
                shield = routeShield
            )
        }
        text = roadNameLabel
    }

    private fun SpannableStringBuilder.renderRoadName(
        text: String,
        shield: RouteShield?
    ) {
        ifNonNull(shield) {
            val shieldBuilder = createSpannableBuilderWithShieldOrText(
                fallback = text,
                shield = it
            )
            append(shieldBuilder)
        } ?: append(text)
        append(" ")
    }

    private fun createSpannableBuilderWithShieldOrText(
        fallback: String,
        shield: RouteShield
    ): SpannableStringBuilder {
        val shieldBuilder = SpannableStringBuilder(fallback)
        val bitmap = shield.toBitmap(resources = context.resources, desiredHeight = lineHeight)
        if (bitmap != null) {
            val drawable = bitmap.drawableWithHeight(
                lineHeight,
                context.resources
            )
            shieldBuilder.setSpan(
                ImageSpan(drawable),
                0,
                fallback.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return shieldBuilder
    }
}

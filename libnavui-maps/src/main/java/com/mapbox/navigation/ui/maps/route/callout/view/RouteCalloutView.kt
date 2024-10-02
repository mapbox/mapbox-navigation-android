package com.mapbox.navigation.ui.maps.route.callout.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.mapbox.maps.ViewAnnotationAnchor
import com.mapbox.maps.ViewAnnotationAnchorConfig
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.LocaleEx
import com.mapbox.navigation.base.internal.time.TimeFormatter
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.databinding.MapboxNavigationRouteCalloutBinding
import com.mapbox.navigation.ui.maps.internal.route.callout.model.DurationDifferenceType
import com.mapbox.navigation.ui.maps.internal.route.callout.model.RouteCallout
import com.mapbox.navigation.ui.maps.route.callout.model.MapboxRouteCalloutViewOptions

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RouteCalloutView : FrameLayout {

    internal constructor(
        context: Context,
        options: MapboxRouteCalloutViewOptions,
    ) : this(context, null, -1, options)

    @JvmOverloads
    internal constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        options: MapboxRouteCalloutViewOptions = MapboxRouteCalloutViewOptions.Builder().build(),
    ) : super(context, attrs, defStyleAttr) {
        this.options = options
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
        )
        applyOptions()
    }

    private val binding = MapboxNavigationRouteCalloutBinding.inflate(
        LayoutInflater.from(context),
        this,
    )

    private var options: MapboxRouteCalloutViewOptions
    private var routeCallout: RouteCallout? = null

    @ColorInt
    private var backgroundColor = ContextCompat.getColor(context, R.color.colorSurface)

    @ColorInt
    private var selectedBackgroundColor =
        ContextCompat.getColor(context, R.color.mapbox_selected_route_callout_background)

    @ColorInt
    private var textColor = ContextCompat.getColor(context, R.color.colorOnSurface)

    @ColorInt
    private var selectedTextColor = ContextCompat.getColor(context, R.color.colorOnSecondary)

    @ColorInt
    private var slowerTextColor =
        ContextCompat.getColor(context, R.color.mapbox_slower_route_callout_text)

    @ColorInt
    private var fasterTextColor = ContextCompat.getColor(
        context,
        R.color.mapbox_faster_route_callout_text,
    )

    internal fun updateOptions(viewOptions: MapboxRouteCalloutViewOptions) {
        options = viewOptions

        applyOptions()
    }

    private fun applyOptions() {
        TextViewCompat.setTextAppearance(
            binding.eta,
            options.durationTextAppearance,
        )

        TextViewCompat.setTextAppearance(
            binding.shape,
            options.durationTextAppearance,
        )

        backgroundColor = ContextCompat.getColor(context, options.backgroundColor)
        selectedBackgroundColor = ContextCompat.getColor(context, options.selectedBackgroundColor)

        textColor = ContextCompat.getColor(context, options.textColor)
        selectedTextColor = ContextCompat.getColor(context, options.selectedTextColor)

        slowerTextColor = ContextCompat.getColor(context, options.slowerTextColor)
        fasterTextColor = ContextCompat.getColor(context, options.fasterTextColor)

        updateColors()
    }

    private fun updateColors() {
        val callout: RouteCallout = routeCallout ?: return

        binding.eta.background?.setTint(
            if ((callout as? RouteCallout.Eta)?.isPrimary == true) {
                selectedBackgroundColor
            } else {
                backgroundColor
            },
        )

        when (callout) {
            is RouteCallout.Eta -> {
                val textColor = if (callout.isPrimary) {
                    selectedTextColor
                } else {
                    this.textColor
                }
                binding.eta.setTextColor(textColor)
            }

            is RouteCallout.DurationDifference -> {
                val textColor = when (callout.type) {
                    DurationDifferenceType.Faster -> fasterTextColor
                    DurationDifferenceType.Slower -> slowerTextColor
                    DurationDifferenceType.Same -> this.textColor
                }
                binding.eta.setTextColor(textColor)
            }
        }
    }

    internal fun setRouteCallout(callout: RouteCallout) {
        this.routeCallout = callout
        val route = callout.route
        val locale = LocaleEx.getLocaleDirectionsRoute(route.directionsRoute, context)

        when (callout) {
            is RouteCallout.Eta -> {
                isSelected = callout.isPrimary
                val duration = TimeFormatter.formatTimeRemaining(
                    context,
                    route.directionsRoute.duration(),
                    locale,
                )
                duration.clearSpans()

                binding.eta.text = duration
                binding.shape.text = duration
            }

            is RouteCallout.DurationDifference -> {
                isSelected = false
                val relativeDuration = TimeFormatter.formatTimeRemaining(
                    context,
                    callout.duration.inWholeSeconds.toDouble(),
                    locale,
                )
                val relativeText = when (callout.type) {
                    DurationDifferenceType.Faster -> context.getString(
                        R.string.mapbox_callout_faster,
                        relativeDuration,
                    )

                    DurationDifferenceType.Slower -> context.getString(
                        R.string.mapbox_callout_slower,
                        relativeDuration,
                    )

                    DurationDifferenceType.Same ->
                        context.getString(R.string.mapbox_callout_similar_eta)
                }
                binding.eta.text = relativeText
                binding.shape.text = relativeText
            }
        }

        updateColors()
    }

    internal fun updateAnchor(anchorConfig: ViewAnnotationAnchorConfig) {
        val (shapeId, shadowId) = when (anchorConfig.anchor) {
            ViewAnnotationAnchor.BOTTOM_RIGHT -> {
                R.drawable.mapbox_ic_route_callout_bottom_right to
                    R.drawable.mapbox_ic_route_callout_bottom_right_shadow
            }

            ViewAnnotationAnchor.TOP_RIGHT -> {
                R.drawable.mapbox_ic_route_callout_top_right to
                    R.drawable.mapbox_ic_route_callout_top_right_shadow
            }

            ViewAnnotationAnchor.BOTTOM_LEFT -> {
                R.drawable.mapbox_ic_route_callout_bottom_left to
                    R.drawable.mapbox_ic_route_callout_bottom_left_shadow
            }

            else -> {
                R.drawable.mapbox_ic_route_callout_top_left to
                    R.drawable.mapbox_ic_route_callout_top_left_shadow
            }
        }

        val drawableShadow = ContextCompat.getDrawable(context, shadowId)
        val drawableShape = ContextCompat.getDrawable(context, shapeId)

        if ((routeCallout as? RouteCallout.Eta)?.isPrimary == true) {
            drawableShape?.setTint(selectedBackgroundColor)
        } else {
            drawableShape?.setTint(backgroundColor)
        }
        binding.eta.background = drawableShape
        binding.shape.background = drawableShadow
    }
}

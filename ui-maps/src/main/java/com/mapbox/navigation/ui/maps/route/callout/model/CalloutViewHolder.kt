package com.mapbox.navigation.ui.maps.route.callout.model

import android.view.View
import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView

/**
 * Represents a view and its [ViewAnnotationOptions] that are used to be attached to a route line.
 *
 * Note that [ViewAnnotationOptions.annotatedFeature] will be overridden by [MapboxRouteLineView]
 */
@ExperimentalPreviewMapboxNavigationAPI
class CalloutViewHolder private constructor(
    internal val view: View,
    internal val options: ViewAnnotationOptions,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CalloutViewHolder

        if (view != other.view) return false
        if (options != other.options) return false

        return true
    }

    override fun hashCode(): Int {
        var result = view.hashCode()
        result = 31 * result + options.hashCode()
        return result
    }

    override fun toString(): String {
        return "CalloutViewHolder(view=$view, options=$options)"
    }

    fun toBuilder(): Builder = Builder(view).options(options)

    @ExperimentalPreviewMapboxNavigationAPI
    class Builder(private var view: View) {
        private var options: ViewAnnotationOptions = ViewAnnotationOptions.Builder().build()

        fun view(view: View): Builder = apply { this.view = view }
        fun options(options: ViewAnnotationOptions): Builder = apply { this.options = options }
        fun build(): CalloutViewHolder = CalloutViewHolder(view, options)
    }
}

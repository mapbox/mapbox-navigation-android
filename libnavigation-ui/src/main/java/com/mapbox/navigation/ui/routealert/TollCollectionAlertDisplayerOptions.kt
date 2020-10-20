package com.mapbox.navigation.ui.routealert

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.mapbox.navigation.ui.R

/**
 * The options to build a RouteAlert view, refer to [TollCollectionAlertDisplayer].
 *
 * @param context to retrieve drawable/string resources
 * @param drawable the icon which represents a route alert.
 *  Use default icon if is not assigned and is necessary.
 * @param properties customized properties. Don't customize the 'iconImage' and 'textField'
 *  properties. These two properties will be dropped and use the default keys.
 */
class TollCollectionAlertDisplayerOptions private constructor(
    private val context: Context,
    val drawable: Drawable,
    val properties: Array<PropertyValue<out Any>>
) {

    /**
     * @return the builder that created the [TollCollectionAlertDisplayerOptions]
     */
    fun toBuilder() = Builder(context).apply {
        drawable(drawable)
        properties(properties)
    }

    /**
     * Override the equals method. Regenerate whenever a change is made.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TollCollectionAlertDisplayerOptions

        if (context != other.context) return false
        if (drawable != other.drawable) return false
        if (!properties.contentEquals(other.properties)) return false

        return true
    }

    /**
     * Override the hashCode method. Regenerate whenever a change is made.
     */
    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + drawable.hashCode()
        result = 31 * result + properties.contentHashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteAlertViewOptions(" +
            "context=$context, " +
            "drawable=$drawable, " +
            "properties=$properties" +
            ")"
    }

    /**
     * Builder for [TollCollectionAlertDisplayerOptions]
     */
    class Builder(private val context: Context) {
        private var drawable: Drawable =
            ContextCompat.getDrawable(context, R.drawable.mapbox_ic_route_alert_toll)!!
        private var properties: Array<PropertyValue<out Any>> = emptyArray()

        /**
         * Defines the drawable for the route alert symbol if needed
         */
        fun drawable(drawable: Drawable) = this.apply {
            this.drawable = drawable
        }

        /**
         * Defines the properties for the route alert layer
         * <p>
         * If not defined, then the default Mapbox generated properties will
         * apply to the route alert layer.
         */
        fun properties(properties: Array<PropertyValue<out Any>>) = this.apply {
            this.properties = properties
        }

        /**
         * Build the [TollCollectionAlertDisplayerOptions]
         */
        fun build() = TollCollectionAlertDisplayerOptions(context, drawable, properties)
    }
}

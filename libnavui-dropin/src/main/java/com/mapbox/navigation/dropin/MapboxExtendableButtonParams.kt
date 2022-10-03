package com.mapbox.navigation.dropin

import android.widget.LinearLayout
import androidx.annotation.StyleRes

/**
 * Params used to modify button appearance.
 *
 * @param style style res of a button.
 * @param layoutParams layout params of a button.
 * @param enabled `true` if the button should be enabled, `false` otherwise
 */
class MapboxExtendableButtonParams @JvmOverloads constructor(
    @StyleRes
    val style: Int,
    val layoutParams: LinearLayout.LayoutParams,
    val enabled: Boolean = true
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxExtendableButtonParams

        if (style != other.style) return false
        if (layoutParams != other.layoutParams) return false
        if (enabled != other.enabled) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = style
        result = 31 * result + layoutParams.hashCode()
        result = 31 * result + enabled.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxExtendableButtonParams(" +
            "style=$style, " +
            "layoutParams=$layoutParams, " +
            "enabled=$enabled" +
            ")"
    }
}

package com.mapbox.navigation.ui.maps.roadname.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StyleRes
import androidx.core.widget.TextViewCompat
import com.mapbox.navigation.ui.maps.databinding.MapboxRoadNameLabelLayoutBinding
import com.mapbox.navigation.ui.maps.roadname.model.RoadLabel

/**
 * Default Mapbox implementation that allows you to render road name labels and route shields
 * associated with the name.
 */
@Deprecated(
    message = "This view is incapable of rendering multiple shields for a given road name",
    replaceWith = ReplaceWith("MapboxRoadNameView")
)
class MapboxRoadNameLabelView : LinearLayout {

    private val binding = MapboxRoadNameLabelLayoutBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    /**
     * Default view to render a road name label.
     *
     * @see MapboxRoadNameLabelApi
     */
    @Deprecated(
        message = "The constructor will not be supported and will be removed."
    )
    constructor(context: Context) : super(context)

    /**
     * Default view to render a road name label.
     *
     * @see MapboxRoadNameLabelApi
     */
    @Deprecated(
        message = "The constructor will not be supported and will be removed."
    )
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     * Default view to render a road name label.
     *
     * @see MapboxRoadNameLabelApi
     */
    @Deprecated(
        message = "The constructor will not be supported and will be removed."
    )
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    init {
        orientation = HORIZONTAL
    }

    /**
     * Invoke the method to render the road name label and route shield icon
     * @param roadLabel RoadLabel
     */
    @Deprecated(
        message = "The method will not be supported and will be removed."
    )
    fun render(roadLabel: RoadLabel) {
        showShieldIcon(roadLabel.shield != null)
        binding.roadNameLabel.text = roadLabel.roadName
    }

    /**
     * Invoke the function to show or hide the road name text associated with the view.
     * @param show Boolean makes the text [View.VISIBLE] if true, [View.GONE] if false
     */
    @Deprecated(
        message = "The method will not be supported and will be removed."
    )
    fun showRoadName(show: Boolean) {
        binding.roadNameLabel.visibility = when (show) {
            true -> { VISIBLE }
            else -> { GONE }
        }
    }

    /**
     * Invoke the function to show or hide the route shield associated with the view.
     * @param show Boolean makes the shield [View.VISIBLE] if true, [View.GONE] if false
     */
    @Deprecated(
        message = "The method will not be supported and will be removed."
    )
    fun showShieldIcon(show: Boolean) {
        binding.roadNameShieldIcon.visibility = when (show) {
            true -> { VISIBLE }
            else -> { GONE }
        }
    }

    /**
     * Allows you to change the text appearance of road name label text.
     * @see [TextViewCompat.setTextAppearance]
     * @param style Int
     */
    @Deprecated(
        message = "The method will not be supported and will be removed."
    )
    fun updateRoadNameTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(binding.roadNameLabel, style)
    }
}

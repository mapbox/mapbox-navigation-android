package com.mapbox.navigation.dropin.binder.roadlabel

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.viewModelScope
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.roadlabel.RoadNameComponentContractImpl
import com.mapbox.navigation.dropin.databinding.MapboxRoadNameLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.maps.internal.ui.RoadNameComponent
import com.mapbox.navigation.ui.maps.roadname.view.MapboxRoadNameView

@ExperimentalPreviewMapboxNavigationAPI
internal class RoadNameViewBinder(
    private val context: NavigationViewContext
) : UIBinder {

    private val loadedMapStyle = context.mapStyleLoader.loadedMapStyle

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_road_name_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = MapboxRoadNameLayoutBinding.bind(viewGroup)

        return reloadOnChange(
            loadedMapStyle,
            context.styles.roadNameTextAppearance,
            context.styles.roadNameBackground
        ) { style, appearance, background ->
            if (style != null) {
                roadNameComponent(binding.roadNameView, style, appearance, background)
            } else {
                null
            }
        }
    }

    private fun roadNameComponent(
        roadNameView: MapboxRoadNameView,
        mapStyle: Style,
        @StyleRes textAppearance: Int,
        @DrawableRes roadNameBackground: Int,
    ): RoadNameComponent {
        roadNameView.background = ContextCompat.getDrawable(
            roadNameView.context,
            roadNameBackground
        )
        TextViewCompat.setTextAppearance(roadNameView, textAppearance)

        val contract = RoadNameComponentContractImpl(
            mapStyle,
            context.viewModel.viewModelScope,
            context.store
        )
        return RoadNameComponent(
            roadNameView,
            { contract }
        )
    }
}

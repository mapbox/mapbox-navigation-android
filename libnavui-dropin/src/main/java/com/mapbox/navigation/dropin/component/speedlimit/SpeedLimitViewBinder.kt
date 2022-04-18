package com.mapbox.navigation.dropin.component.speedlimit

import android.view.ViewGroup
import androidx.transition.Fade
import androidx.transition.Scene
import androidx.transition.TransitionManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.binder.UIBinder
import com.mapbox.navigation.dropin.databinding.MapboxSpeedLimitLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange

@ExperimentalPreviewMapboxNavigationAPI
internal class SpeedLimitViewBinder(
    val context: NavigationViewContext
) : UIBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_speed_limit_layout,
            viewGroup.context
        )
        TransitionManager.go(scene, Fade())
        val binding = MapboxSpeedLimitLayoutBinding.bind(viewGroup)

        return reloadOnChange(
            context.styles.speedLimitStyle,
            context.styles.speedLimitTextAppearance
        ) { style, textAppearance ->
            SpeedLimitComponent(
                style = style,
                textAppearance = textAppearance,
                speedLimitView = binding.speedLimitView,
            )
        }
    }
}

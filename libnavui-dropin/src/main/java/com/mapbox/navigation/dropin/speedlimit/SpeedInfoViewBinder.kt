package com.mapbox.navigation.dropin.speedlimit

import android.view.ViewGroup
import androidx.transition.Fade
import androidx.transition.Scene
import androidx.transition.TransitionManager
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxSpeedInfoLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.speedlimit.internal.SpeedInfoComponent

internal class SpeedInfoViewBinder(
    val context: NavigationViewContext
) : UIBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_speed_info_layout,
            viewGroup.context
        )
        TransitionManager.go(scene, Fade())
        val binding = MapboxSpeedInfoLayoutBinding.bind(viewGroup)

        return reloadOnChange(
            context.styles.speedInfoOptions,
            context.options.distanceFormatterOptions,
        ) { options, distanceFormatter ->
            SpeedInfoComponent(
                speedInfoOptions = options,
                speedInfoView = binding.speedInfoView,
                distanceFormatterOptions = distanceFormatter,
            )
        }
    }
}

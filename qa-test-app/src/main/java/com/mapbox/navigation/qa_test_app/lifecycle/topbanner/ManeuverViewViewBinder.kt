package com.mapbox.navigation.qa_test_app.lifecycle.topbanner

import android.view.ViewGroup
import androidx.core.view.get
import androidx.transition.Scene
import androidx.transition.TransitionManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.lifecycle.DropInViewBinder
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.DropInManeuverViewBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ManeuverViewViewBinder : DropInViewBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.drop_in_maneuver_view,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = DropInManeuverViewBinding.bind(viewGroup[0])
        return DropInManeuver(binding.root)
    }
}

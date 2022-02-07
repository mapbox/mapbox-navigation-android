package com.mapbox.navigation.qa_test_app.lifecycle.bottomsheet

import android.view.ViewGroup
import androidx.core.view.get
import androidx.transition.Scene
import androidx.transition.TransitionManager
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.lifecycle.DropInViewBinder
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.DropInRoutePreviewBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoutePreviewViewBinder(val routes: List<DirectionsRoute>) : DropInViewBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.drop_in_route_preview,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = DropInRoutePreviewBinding.bind(viewGroup[0])
        return DropInRoutePreview(
            routes,
            binding.routesContainer,
            binding.clear,
            binding.startNavigation
        )
    }
}

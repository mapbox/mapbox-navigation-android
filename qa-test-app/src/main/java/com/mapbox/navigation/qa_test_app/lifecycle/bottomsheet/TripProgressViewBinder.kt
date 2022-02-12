package com.mapbox.navigation.qa_test_app.lifecycle.bottomsheet

import android.view.ViewGroup
import androidx.core.view.get
import androidx.transition.Scene
import androidx.transition.TransitionManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.lifecycle.DropInViewBinder
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.DropInTripProgressBinding

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class TripProgressViewBinder : DropInViewBinder {
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.drop_in_trip_progress,
            viewGroup.context
        )
        TransitionManager.go(scene)
        val binding = DropInTripProgressBinding.bind(viewGroup[0])
        return DropInTripProgress(
            binding.stop,
            binding.tripProgressView
        )
    }
}

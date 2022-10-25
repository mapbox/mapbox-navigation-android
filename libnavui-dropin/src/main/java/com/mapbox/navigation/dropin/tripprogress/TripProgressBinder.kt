package com.mapbox.navigation.dropin.tripprogress

import android.transition.Scene
import android.transition.TransitionManager
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxTripProgressViewLayoutBinding
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.tripprogress.internal.ui.TripProgressComponent
import com.mapbox.navigation.ui.tripprogress.model.DistanceRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.ui.tripprogress.model.TimeRemainingFormatter
import com.mapbox.navigation.ui.tripprogress.model.TripProgressUpdateFormatter

@ExperimentalPreviewMapboxNavigationAPI
internal class TripProgressBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val scene = Scene.getSceneForLayout(
            viewGroup,
            R.layout.mapbox_trip_progress_view_layout,
            viewGroup.context
        )
        TransitionManager.go(scene)

        val binding = MapboxTripProgressViewLayoutBinding.bind(viewGroup)
        return reloadOnChange(
            context.styles.tripProgressStyle,
            context.options.distanceFormatterOptions
        ) { styles, distanceFormatterOptions ->
            binding.tripProgressView.updateStyle(styles)

            val contract = TripProgressComponentContractImpl(
                context.viewModel.viewModelScope,
                context.store,
            )
            val formatter = TripProgressUpdateFormatter.Builder(binding.tripProgressView.context)
                .distanceRemainingFormatter(DistanceRemainingFormatter(distanceFormatterOptions))
                .timeRemainingFormatter(TimeRemainingFormatter(binding.tripProgressView.context))
                .estimatedTimeToArrivalFormatter(
                    EstimatedTimeToArrivalFormatter(binding.tripProgressView.context)
                )
                .build()

            TripProgressComponent(
                tripProgressView = binding.tripProgressView,
                contactProvider = { contract },
                tripProgressFormatter = formatter
            )
        }
    }
}

package com.mapbox.navigation.dropin

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.MapView

/**
 * This context is a top level data object for [DropInNavigationView].
 *
 * If your data should survive orientation changes, place it inside [DropInNavigationViewModel].
 */
internal class DropInNavigationViewContext(
    val lifecycleOwner: LifecycleOwner,
    val viewModel: DropInNavigationViewModel,
    val mapView: MapView,
    val viewGroup: ViewGroup,
)

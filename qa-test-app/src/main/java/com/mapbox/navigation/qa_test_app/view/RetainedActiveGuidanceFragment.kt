package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.asLiveData
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.dropin.internal.extensions.attachStarted
import com.mapbox.navigation.dropin.internal.extensions.flowRoutesUpdated
import com.mapbox.navigation.dropin.internal.extensions.flowTripSessionState
import com.mapbox.navigation.qa_test_app.databinding.FragmentActiveGuidanceBinding
import com.mapbox.navigation.qa_test_app.lifecycle.bottomsheet.DropInTripProgress
import com.mapbox.navigation.qa_test_app.lifecycle.topbanner.DropInManeuver
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RetainedActiveGuidanceFragment : Fragment() {

    private var dropInTripProgress: DropInTripProgress? = null
    private var dropInManeuver: DropInManeuver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retaining the fragment is deprecated and not needed, but this example is to show
        // how to build a non-leaking retained fragment.
        retainInstance = true

        // Because this component has an entirely different LifecycleOwner,
        // attach it to the MapboxNavigationApp to ensure the DropInActiveGuidance will be
        // able to connect to MapboxNavigation
        MapboxNavigationApp.attach(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val binding = FragmentActiveGuidanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun MapboxNavigation.flowActiveGuidanceStarted() = combine(
        flowTripSessionState(), flowRoutesUpdated()
    ) { tripSessionState, routesUpdatedResult ->
        TripSessionState.STARTED == tripSessionState && routesUpdatedResult.routes.isNotEmpty()
    }.distinctUntilChanged().onStart { emit(false) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentActiveGuidanceBinding.bind(view)

        attachStarted(object : MapboxNavigationObserver {
            override fun onAttached(mapboxNavigation: MapboxNavigation) {
                mapboxNavigation.flowActiveGuidanceStarted().asLiveData()
                    .observe(viewLifecycleOwner) {
                        val visibility = if (it) View.VISIBLE else View.GONE
                        binding.tripProgressCard.visibility = visibility
                        binding.maneuverView.visibility = visibility
                    }
            }

            override fun onDetached(mapboxNavigation: MapboxNavigation) {
                // No op
            }
        })

        // Notice, the this works the same if it is in a Fragment or an Activity.
        dropInTripProgress = DropInTripProgress(
            stopView = binding.stop,
            tripProgressView = binding.tripProgressView
        ).also { MapboxNavigationApp.registerObserver(it) }
        dropInManeuver = DropInManeuver(
            maneuverView = binding.maneuverView,
        ).also { MapboxNavigationApp.registerObserver(it) }
    }

    override fun onDestroyView() {
        // Null and remove. This is needed to avoid memory leaks because multiple views can
        // be created within the Fragment lifecycle.
        dropInTripProgress?.let { MapboxNavigationApp.unregisterObserver(it) }
        dropInTripProgress = null
        dropInManeuver?.let { MapboxNavigationApp.unregisterObserver(it) }
        dropInManeuver = null

        super.onDestroyView()
    }
}

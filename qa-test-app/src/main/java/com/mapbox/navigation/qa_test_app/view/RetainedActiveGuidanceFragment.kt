package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.qa_test_app.databinding.FragmentActiveGuidanceBinding
import com.mapbox.navigation.qa_test_app.lifecycle.DropInActiveGuidance

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RetainedActiveGuidanceFragment : Fragment() {

    private var dropInActiveGuidance: DropInActiveGuidance? = null

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentActiveGuidanceBinding.bind(view)

        // Notice, the `DropInActiveGuidance` works the same if it is in a Fragment or an Activity.
        dropInActiveGuidance = DropInActiveGuidance(
            stopView = binding.stop,
            maneuverView = binding.maneuverView,
            tripProgressView = binding.tripProgressView
        ).also { lifecycle.addObserver(it) }
    }

    override fun onDestroyView() {
        // Null and remove. This is needed to avoid memory leaks because multiple views can
        // be created within the Fragment lifecycle.
        dropInActiveGuidance?.let { lifecycle.removeObserver(it) }
        dropInActiveGuidance = null

        super.onDestroyView()
    }
}

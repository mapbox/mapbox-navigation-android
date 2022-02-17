package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import android.view.View
import com.mapbox.navigation.dropin.view.MapboxCameraModeButton
import com.mapbox.navigation.dropin.view.MapboxExtendableButton
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityDropinButtonsBinding
import com.mapbox.navigation.qa_test_app.view.base.BaseNavigationActivity
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.dropin.R as Mapbox_R

class DropInButtonsActivity : BaseNavigationActivity() {

    private lateinit var binding: LayoutActivityDropinButtonsBinding

    override fun onCreateContentView(): View {
        binding = LayoutActivityDropinButtonsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigationCamera.registerNavigationCameraStateChangeObserver { navigationCameraState ->
            binding.cameraModeButton.setState(navigationCameraState)
            binding.customCameraModeButton.setState(navigationCameraState)
        }

        // init buttons
        binding.cameraModeButton.setState(navigationCamera.state)
        binding.customCameraModeButton.setState(navigationCamera.state)

        binding.cameraModeButton.setOnClickListener(this::onCameraModeButtonClick)
        binding.customCameraModeButton.setOnClickListener(this::onCameraModeButtonClick)

        binding.extButton.doOnClick {
            toggleState = !toggleState
            if (toggleState) it.setState(stateOverviewWithText)
            else it.setState(stateFollowingWithText)
        }
        binding.extButton2.doOnClick {
            toggleState = !toggleState
            if (toggleState) it.setState(stateOverview)
            else it.setState(stateFollowing)
        }
    }

    private val stateOverview =
        MapboxExtendableButton.State(Mapbox_R.drawable.mapbox_ic_camera_overview)
    private val stateFollowing = MapboxExtendableButton.State(
        Mapbox_R.drawable.mapbox_ic_camera_follow
    )
    private val stateOverviewWithText = stateOverview.copy(
        text = "Overview",
        duration = 2000
    )
    private val stateFollowingWithText = stateFollowing.copy(
        text = "Following",
        duration = 2000
    )
    private var toggleState = false

    private inline fun <T : View> T.doOnClick(crossinline action: (v: T) -> Unit) {
        setOnClickListener { action(this) }
    }

    private fun onCameraModeButtonClick(v: View) {
        val button = v as MapboxCameraModeButton
        navigationCamera.apply {
            if (isInFollowMode(state)) {
                requestNavigationCameraToOverview()
                button.setStateAndExtend(NavigationCameraState.OVERVIEW)
            } else {
                requestNavigationCameraToFollowing()
                button.setStateAndExtend(NavigationCameraState.FOLLOWING)
            }
        }
    }

    private fun isInFollowMode(state: NavigationCameraState) =
        state == NavigationCameraState.TRANSITION_TO_FOLLOWING ||
            state == NavigationCameraState.FOLLOWING
}

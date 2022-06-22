package com.mapbox.navigation.qa_test_app.view

import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.lifecycle.MutableLiveData
import com.mapbox.maps.CameraOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.view.MapboxCameraModeButton
import com.mapbox.navigation.dropin.view.MapboxExtendableButton
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityDropinButtonsBinding
import com.mapbox.navigation.qa_test_app.view.base.BaseNavigationActivity
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.voice.view.MapboxAudioGuidanceButton
import com.mapbox.navigation.utils.internal.toPoint

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInButtonsActivity : BaseNavigationActivity() {

    private lateinit var binding: LayoutActivityDropinButtonsBinding

    private lateinit var navigationCameraState: MutableLiveData<NavigationCameraState>
    private lateinit var muted: MutableLiveData<Boolean>

    override fun onCreateContentView(): View {
        binding = LayoutActivityDropinButtonsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigationCameraState = MutableLiveData(navigationCamera.state)
        navigationCamera
            .registerNavigationCameraStateChangeObserver(navigationCameraState::setValue)
        navigationCameraState.observe(this) {
            binding.cameraModeButton.setState(it)
            binding.circleCameraModeButton.setState(it)
            binding.customCameraModeButton.setState(it)
        }

        muted = MutableLiveData(false)
        muted.observe(this) { isMuted ->
            if (isMuted) {
                binding.audioButton.mute()
                binding.circleAudioButton.mute()
            } else {
                binding.audioButton.unMute()
                binding.circleAudioButton.unMute()
            }
        }
    }

    fun onCameraModeButtonClick(v: View) {
        val button = v as? MapboxCameraModeButton ?: return

        navigationCamera.apply {
            if (state.isFollowing()) {
                requestNavigationCameraToOverview()
                button.setStateAndExtend(NavigationCameraState.OVERVIEW)
            } else {
                requestNavigationCameraToFollowing()
                button.setStateAndExtend(NavigationCameraState.FOLLOWING)
            }
        }
    }

    fun onAudioButtonClick(v: View) {
        val button = v as? MapboxAudioGuidanceButton ?: return

        if (muted.value == true) {
            muted.value = false
            button.unMuteAndExtend()
        } else {
            muted.value = true
            button.muteAndExtend()
        }
    }

    fun onRecenterButtonClick(v: View) {
        val button = v as? MapboxExtendableButton ?: return
        val location = lastLocation ?: return

        camera.easeTo(centeredAt(location))
        button.setState(
            MapboxExtendableButton.State(
                R.drawable.mapbox_ic_camera_recenter,
                "Recenter",
                2000
            )
        )
    }

    private fun NavigationCameraState.isFollowing() =
        this == NavigationCameraState.TRANSITION_TO_FOLLOWING ||
            this == NavigationCameraState.FOLLOWING

    private fun centeredAt(location: Location) =
        CameraOptions.Builder().center(location.toPoint()).build()
}

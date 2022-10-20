package com.mapbox.navigation.dropin.actionbutton

import android.view.LayoutInflater
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.R

/**
 * Default Action Buttons Binder implementation.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxActionButtonsBinder : ActionButtonsBinder() {

    override fun onCreateLayout(layoutInflater: LayoutInflater, root: ViewGroup): ViewGroup =
        layoutInflater
            .inflate(R.layout.mapbox_action_buttons_layout, root, false) as ViewGroup

    override fun getCustomButtonsStartContainer(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.buttonsContainerTop)

    override fun getCompassButtonContainer(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.buttonsContainerCompass)

    override fun getCameraModeButtonContainer(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.buttonsContainerCamera)

    override fun getToggleAudioButtonContainer(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.buttonsContainerAudio)

    override fun getRecenterButtonContainer(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.buttonsContainerRecenter)

    override fun getCustomButtonsEndContainer(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.buttonsContainerBottom)
}

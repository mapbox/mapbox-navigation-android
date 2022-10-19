package com.mapbox.navigation.qa_test_app.view.customnavview

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.actionbutton.ActionButtonsBinder
import com.mapbox.navigation.qa_test_app.R

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CustomActionButtonsBinder : ActionButtonsBinder() {

    override fun onCreateLayout(
        layoutInflater: LayoutInflater,
        root: ViewGroup
    ): ViewGroup =
        layoutInflater
            .inflate(R.layout.layout_action_buttons, root, false) as ViewGroup

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

    override fun verticalSpacing(resources: Resources): Int = 10.dp
}

package com.mapbox.navigation.dropin.actionbutton

import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.camera.CameraModeButtonComponentContractImpl
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.maps.internal.ui.CameraModeButtonComponent
import com.mapbox.navigation.ui.maps.view.MapboxCameraModeButton

@ExperimentalPreviewMapboxNavigationAPI
internal class CameraModeButtonBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        return reloadOnChange(context.styles.cameraModeButtonParams) { params ->
            val button = MapboxCameraModeButton(viewGroup.context, null, 0, params.style)
            viewGroup.removeAllViews()
            viewGroup.addView(button, params.layoutParams)

            CameraModeButtonComponent(button) {
                CameraModeButtonComponentContractImpl(
                    context.viewModel.viewModelScope,
                    context.store
                )
            }
        }
    }
}

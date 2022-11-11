package com.mapbox.navigation.dropin.actionbutton

import android.view.ViewGroup
import androidx.annotation.Px
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.camera.CameraModeButtonComponentContractImpl
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.internal.extensions.updateMargins
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.maps.internal.ui.CameraModeButtonComponent
import com.mapbox.navigation.ui.maps.view.MapboxCameraModeButton

internal class CameraModeButtonBinder(
    private val context: NavigationViewContext,
    @Px private val verticalSpacing: Int = 0
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        return reloadOnChange(context.styles.cameraModeButtonStyle) { style ->
            val button = MapboxCameraModeButton(viewGroup.context, null, 0, style)
            viewGroup.removeAllViews()
            viewGroup.addView(button)
            button.updateMargins(top = verticalSpacing, bottom = verticalSpacing)

            CameraModeButtonComponent(button) {
                CameraModeButtonComponentContractImpl(
                    context.viewModel.viewModelScope,
                    context.store
                )
            }
        }
    }
}

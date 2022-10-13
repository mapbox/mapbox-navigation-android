package com.mapbox.navigation.dropin.actionbutton

import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.camera.RecenterButtonComponentContractImpl
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import com.mapbox.navigation.ui.maps.internal.ui.RecenterButtonComponent

@ExperimentalPreviewMapboxNavigationAPI
internal class RecenterButtonBinder(
    private val context: NavigationViewContext
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        return reloadOnChange(context.styles.recenterButtonParams) { params ->
            val button = MapboxExtendableButton(viewGroup.context, null, 0, params.style)
            viewGroup.removeAllViews()
            viewGroup.addView(button, params.layoutParams)

            RecenterButtonComponent(button) {
                RecenterButtonComponentContractImpl(context.viewModel.viewModelScope, context.store)
            }
        }
    }
}

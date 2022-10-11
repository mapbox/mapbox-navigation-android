package com.mapbox.navigation.dropin.actionbutton

import android.view.ViewGroup
import androidx.annotation.Px
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.camera.RecenterButtonComponentContractImpl
import com.mapbox.navigation.dropin.internal.extensions.reloadOnChange
import com.mapbox.navigation.dropin.internal.extensions.updateMargins
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.ui.base.view.MapboxExtendableButton
import com.mapbox.navigation.ui.maps.internal.ui.RecenterButtonComponent

internal class RecenterButtonBinder(
    private val context: NavigationViewContext,
    @Px private val verticalSpacing: Int = 0
) : UIBinder {

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        return reloadOnChange(context.styles.recenterButtonStyle) { style ->
            val button = MapboxExtendableButton(viewGroup.context, null, 0, style)
            viewGroup.removeAllViews()
            viewGroup.addView(button)
            button.updateMargins(top = verticalSpacing, bottom = verticalSpacing)

            RecenterButtonComponent(button) {
                RecenterButtonComponentContractImpl(context.viewModel.viewModelScope, context.store)
            }
        }
    }
}

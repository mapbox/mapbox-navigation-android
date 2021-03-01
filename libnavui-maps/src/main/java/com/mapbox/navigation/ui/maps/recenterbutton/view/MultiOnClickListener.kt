package com.mapbox.navigation.ui.maps.recenterbutton.view

import android.view.View

internal class MultiOnClickListener : View.OnClickListener {

    private val onClickListeners: MutableSet<View.OnClickListener>

    fun addListener(onClickListener: View.OnClickListener) {
        onClickListeners.add(onClickListener)
    }

    fun removeListener(onClickListener: View.OnClickListener) {
        onClickListeners.remove(onClickListener)
    }

    fun clearListeners() {
        onClickListeners.clear()
    }

    override fun onClick(view: View) {
        for (onClickListener in onClickListeners) {
            onClickListener.onClick(view)
        }
    }

    init {
        onClickListeners = HashSet()
    }
}
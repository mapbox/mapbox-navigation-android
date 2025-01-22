package com.mapbox.navigation.core.testutil

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

internal class TestLifecycleOwner : LifecycleOwner {
    val lifecycleRegistry = LifecycleRegistry(this)
        .also { it.currentState = Lifecycle.State.INITIALIZED }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    fun moveToState(state: Lifecycle.State) {
        lifecycleRegistry.currentState = state
    }
}

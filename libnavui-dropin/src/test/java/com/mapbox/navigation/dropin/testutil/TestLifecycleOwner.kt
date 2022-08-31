package com.mapbox.navigation.dropin.testutil

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

internal class TestLifecycleOwner : LifecycleOwner {
    @Suppress("MemberVisibilityCanBePrivate")
    val lifecycleRegistry = LifecycleRegistry(this)
        .also { it.currentState = Lifecycle.State.INITIALIZED }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
}

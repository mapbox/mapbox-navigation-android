package com.mapbox.services.android.navigation.v5.navigation

internal abstract class Counter<N : Number>(
    protected val name: String,
    protected val value: N
)

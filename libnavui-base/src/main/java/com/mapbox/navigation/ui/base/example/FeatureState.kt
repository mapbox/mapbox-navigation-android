package com.mapbox.navigation.ui.base.example

sealed class FeatureState<out V>
data class Enabled<out V>(val value: V) : FeatureState<V>()
object Disabled : FeatureState<Nothing>()

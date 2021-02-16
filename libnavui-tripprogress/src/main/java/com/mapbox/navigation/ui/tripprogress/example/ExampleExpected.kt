package com.mapbox.navigation.ui.tripprogress.example

import com.mapbox.navigation.ui.base.example.FeatureState

class ExampleValue internal constructor(
    val mainFeature: Int,
    val additionalFeature: FeatureState<Double>
) {
    fun toMutableValue() = MutableExampleValue(mainFeature, additionalFeature)
}

class ExampleError internal constructor(
    val errorMessage: String,
    val throwable: Throwable? = null
)

class MutableExampleValue internal constructor(
    var mainFeature: Int,
    var additionalFeature: FeatureState<Double>
) {
    fun toImmutableValue() = ExampleValue(mainFeature, additionalFeature)
}

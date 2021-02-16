package com.mapbox.navigation.ui.tripprogress.example

class AnotherExampleValue internal constructor(
    val mainFeature: String
)

class AnotherExampleError internal constructor(
    val errorMessage: String,
    val throwable: Throwable? = null
)

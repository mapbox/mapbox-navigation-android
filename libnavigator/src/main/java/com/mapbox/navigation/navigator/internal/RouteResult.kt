package com.mapbox.navigation.navigator.internal

import androidx.annotation.NonNull
import com.mapbox.bindgen.Expected
import com.mapbox.navigator.RouterError
import com.mapbox.navigator.RouterOrigin

data class RouteResult(val result: Expected<RouterError, String> , val origin: RouterOrigin)

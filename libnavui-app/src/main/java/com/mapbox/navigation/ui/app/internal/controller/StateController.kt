package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.app.internal.Reducer
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
abstract class StateController : UIComponent(), Reducer

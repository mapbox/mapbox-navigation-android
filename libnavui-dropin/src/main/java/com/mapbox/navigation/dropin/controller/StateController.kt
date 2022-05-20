package com.mapbox.navigation.dropin.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.model.Reducer
import com.mapbox.navigation.ui.base.lifecycle.UIComponent

@ExperimentalPreviewMapboxNavigationAPI
internal abstract class StateController : UIComponent(), Reducer

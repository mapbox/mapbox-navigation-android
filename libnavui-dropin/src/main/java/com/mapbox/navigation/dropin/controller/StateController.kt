package com.mapbox.navigation.dropin.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import com.mapbox.navigation.dropin.model.Reducer

@ExperimentalPreviewMapboxNavigationAPI
internal abstract class StateController : UIComponent(), Reducer

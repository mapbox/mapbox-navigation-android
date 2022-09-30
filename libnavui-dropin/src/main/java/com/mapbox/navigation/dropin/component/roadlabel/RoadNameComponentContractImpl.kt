package com.mapbox.navigation.dropin.component.roadlabel

import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.maps.internal.ui.RoadNameComponentContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@ExperimentalPreviewMapboxNavigationAPI
internal class RoadNameComponentContractImpl(
    mapStyle: Style,
    coroutineScope: CoroutineScope,
    store: Store
) : RoadNameComponentContract {

    override val roadInfo: StateFlow<Road?> =
        store.slice(coroutineScope) { it.location?.road }

    override val mapStyle: StateFlow<Style?> = MutableStateFlow(mapStyle)
}

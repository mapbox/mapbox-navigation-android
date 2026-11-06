package com.mapbox.navigation.ui.androidauto.placeslistonmap

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.internal.setHeaderCompat

internal fun PlacesListOnMapManager.buildListTemplate(carContext: CarContext): ListTemplate =
    ListTemplate.Builder()
        .setHeaderCompat(carContext, title = null, startAction = Action.BACK)
        .apply {
            val itemList = itemListOrNull(carContext)
            if (itemList == null) setLoading(true) else setSingleList(itemList)
        }
        .build()

internal fun PlacesListOnMapManager.itemListOrNull(carContext: CarContext): ItemList? =
    when (val state = state.value) {
        PlacesListState.Loading -> null
        is PlacesListState.Loaded -> state.itemList.takeIf { it.items.isNotEmpty() }
            ?: emptyPlacesList(carContext, R.string.car_search_no_results)
        PlacesListState.Failed -> emptyPlacesList(carContext, R.string.car_search_error)
    }

private fun emptyPlacesList(carContext: CarContext, messageRes: Int): ItemList =
    ItemList.Builder().setNoItemsMessage(carContext.getString(messageRes)).build()

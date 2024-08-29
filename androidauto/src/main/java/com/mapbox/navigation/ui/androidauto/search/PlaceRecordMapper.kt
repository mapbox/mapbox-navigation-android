package com.mapbox.navigation.ui.androidauto.search

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.search.record.FavoriteRecord
import com.mapbox.search.result.SearchAddress
import com.mapbox.search.result.SearchResult

internal object PlaceRecordMapper {
    fun fromSearchResult(searchResult: SearchResult): PlaceRecord {
        return PlaceRecord(
            id = searchResult.id,
            name = searchResult.name,
            coordinate = searchResult.coordinate,
            description = searchResult.descriptionText
                ?: mapDescriptionFromAddress(searchResult.address),
            categories = searchResult.categories.orEmpty(),
        )
    }

    fun fromFavoriteRecord(favoriteRecord: FavoriteRecord): PlaceRecord {
        return PlaceRecord(
            id = favoriteRecord.id,
            name = favoriteRecord.name,
            coordinate = favoriteRecord.coordinate,
            description = favoriteRecord.descriptionText
                ?: mapDescriptionFromAddress(favoriteRecord.address),
            categories = favoriteRecord.categories ?: emptyList(),
        )
    }

    private fun mapDescriptionFromAddress(address: SearchAddress?): String? =
        ifNonNull(address?.houseNumber, address?.street) { houseNumber, street ->
            "$houseNumber $street"
        }

    fun fromCarmenFeature(carmenFeature: CarmenFeature): PlaceRecord {
        return PlaceRecord(
            id = carmenFeature.id() ?: "",
            name = carmenFeature.text() ?: "",
            coordinate = carmenFeature.center(),
            description = carmenFeature.placeName(),
            categories = carmenFeature.placeType() ?: emptyList(),
        )
    }

    private inline fun <R1, R2, T> ifNonNull(r1: R1?, r2: R2?, func: (R1, R2) -> T): T? =
        if (r1 != null && r2 != null) {
            func(r1, r2)
        } else {
            null
        }
}

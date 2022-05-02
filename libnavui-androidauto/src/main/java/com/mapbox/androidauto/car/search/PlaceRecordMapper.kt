package com.mapbox.androidauto.car.search

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.search.record.FavoriteRecord
import com.mapbox.search.result.SearchAddress
import com.mapbox.search.result.SearchResult

object PlaceRecordMapper {
    fun fromSearchResult(searchResult: SearchResult): PlaceRecord {
        return PlaceRecord(
            id = searchResult.id,
            name = searchResult.name,
            coordinate = searchResult.coordinate,
            description = searchResult.descriptionText
                ?: mapDescriptionFromAddress(searchResult.address),
            categories = searchResult.categories
        )
    }

    fun fromFavoriteRecord(favoriteRecord: FavoriteRecord): PlaceRecord {
        return PlaceRecord(
            id = favoriteRecord.id,
            name = favoriteRecord.name,
            coordinate = favoriteRecord.coordinate,
            description = favoriteRecord.descriptionText
                ?: mapDescriptionFromAddress(favoriteRecord.address),
            categories = favoriteRecord.categories ?: emptyList()
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
            categories = carmenFeature.placeType() ?: emptyList()
        )
    }
}

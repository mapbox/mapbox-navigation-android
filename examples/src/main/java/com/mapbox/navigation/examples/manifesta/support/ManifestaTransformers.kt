package com.mapbox.navigation.examples.manifesta.support

import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.firestore.GeoPoint
import com.mapbox.geojson.Point
import com.mapbox.navigation.examples.manifesta.model.entity.LocationCollectionEntity
import com.mapbox.navigation.examples.manifesta.model.entity.ManifestaLocation
import com.mapbox.navigation.examples.manifesta.model.entity.ManifestaUser

object ManifestaTransformers {

    const val MANIFESTA_FIELD_ID = "id"
    const val MANIFESTA_FIELD_NAME = "name"
    const val MANIFESTA_FIELD_ADDRESS = "address"
    const val MANIFESTA_FIELD_LOCATION = "location"
    const val MANIFESTA_FIELD_GEOHASH = "geoHash"
    const val MANIFESTA_FIELD_NOTES = "notes"
    const val MANIFESTA_FIELD_TAGS = "tags"
    const val MANIFESTA_FIELD_LOCATION_IDS = "locationIds"
    const val MANIFESTA_FIELD_ALIAS = "alias"
    const val MANIFESTA_FIELD_LOCATION_COLLECTION_ID = "locationCollectionIds"

    fun placeToMap(place: ManifestaLocation): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map[MANIFESTA_FIELD_ID] = place.id
        map[MANIFESTA_FIELD_NAME] = place.name
        map[MANIFESTA_FIELD_ADDRESS] = place.address
        map[MANIFESTA_FIELD_LOCATION] = GeoPoint(place.position.latitude() , place.position.longitude())
        map[MANIFESTA_FIELD_NOTES] = place.notes
        map[MANIFESTA_FIELD_TAGS] = place.tags.joinToString(",")
        // todo the GeoHash calculation will throw an exception if the lat/lon are not valid
        map[MANIFESTA_FIELD_GEOHASH] = GeoFireUtils.getGeoHashForLocation(
            GeoLocation(place.position.latitude(), place.position.longitude())
        )
        return map
    }

    fun mapToPlace(itemMap: Map<String, Any>): ManifestaLocation {
        val locGeoPoint = getMapFieldValueAsGeoPoint(itemMap,MANIFESTA_FIELD_LOCATION)
        return ManifestaLocation(
            getMapFieldValueAsString(itemMap, MANIFESTA_FIELD_ID),
            getMapFieldValueAsString(itemMap, MANIFESTA_FIELD_NAME),
            getMapFieldValueAsString(itemMap, MANIFESTA_FIELD_ADDRESS),
            getMapFieldValueAsString(itemMap, MANIFESTA_FIELD_NOTES),
            Point.fromLngLat(locGeoPoint.longitude, locGeoPoint.latitude),
            getMapFieldValueAsString(itemMap, MANIFESTA_FIELD_GEOHASH),
            getMapFieldValueAsString(itemMap, MANIFESTA_FIELD_TAGS).split(",")
        )
    }

    fun locationCollectionToMap(locationCollection: LocationCollectionEntity): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map[MANIFESTA_FIELD_ID] = locationCollection.id
        map[MANIFESTA_FIELD_NAME] = locationCollection.name
        map[MANIFESTA_FIELD_LOCATION_IDS] = locationCollection.locations.joinToString(",")
        return map
    }

    fun mapToLocationCollection(itemMap: Map<String, Any>): LocationCollectionEntity {
        return LocationCollectionEntity(
            getMapFieldValueAsString(itemMap, MANIFESTA_FIELD_ID),
            getMapFieldValueAsString(itemMap, MANIFESTA_FIELD_NAME),
            getMapFieldValueAsString(itemMap, MANIFESTA_FIELD_LOCATION_IDS).split(",")
        )
    }

    fun ManifestaUser.userToMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map[MANIFESTA_FIELD_ID] = this.id
        map[MANIFESTA_FIELD_ALIAS] = this.alias
        map[MANIFESTA_FIELD_LOCATION_COLLECTION_ID] = this.locationCollections.joinToString(",")
        return map
    }

    fun mapToUser(itemMap: Map<String, Any>): ManifestaUser {
        return ManifestaUser(
            getMapFieldValueAsString(itemMap, MANIFESTA_FIELD_ID),
            getMapFieldValueAsString(itemMap, MANIFESTA_FIELD_ALIAS),
            getMapFieldValueAsString(itemMap, MANIFESTA_FIELD_LOCATION_COLLECTION_ID).split(",")
        )
    }

    fun getMapFieldValueAsGeoPoint(itemMap: Map<String, Any>, fieldName: String): GeoPoint = when {
        !itemMap.containsKey(fieldName) -> GeoPoint(0.0,0.0)
        itemMap[fieldName] == null -> GeoPoint(0.0,0.0)
        else -> itemMap[MANIFESTA_FIELD_LOCATION] as GeoPoint
    }

    fun getMapFieldValueAsString(itemMap: Map<String, Any>, fieldName: String): String = when {
        !itemMap.containsKey(fieldName) -> ""
        itemMap[fieldName] == null -> ""
        else -> itemMap[fieldName] as String
    }
}

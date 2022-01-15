package com.mapbox.navigation.examples.manifesta.model.entity

data class LocationCollectionEntity(
    val id: String,
    val name: String,
    val locations: List<String>
) {
    // a short cut for the list of location collections UI
    override fun toString(): String {
        return name
    }
}

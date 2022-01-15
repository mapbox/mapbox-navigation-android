package com.mapbox.navigation.examples.manifesta.model.entity

data class ManifestaUser(
    val id: String,
    val alias: String,
    val locationCollections: List<String>
) {
    // a short cut for the user spinner in the UI
    override fun toString(): String {
        return alias
    }
}

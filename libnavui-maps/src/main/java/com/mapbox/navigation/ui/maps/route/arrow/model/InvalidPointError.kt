package com.mapbox.navigation.ui.maps.route.arrow.model

/**
 * Represents an error indicating the points provided are invalid.
 *
 * @param errorMessage an error message describing the error.
 * @param throwable an optional throwable related to the error.
 */
class InvalidPointError constructor(val errorMessage: String, val throwable: Throwable?)

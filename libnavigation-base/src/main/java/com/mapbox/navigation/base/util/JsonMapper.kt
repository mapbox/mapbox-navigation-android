package com.mapbox.navigation.base.util

interface JsonMapper {
    fun <T> toJson(obj: T): String
}

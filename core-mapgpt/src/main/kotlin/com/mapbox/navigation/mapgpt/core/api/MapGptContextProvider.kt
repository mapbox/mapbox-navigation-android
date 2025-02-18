package com.mapbox.navigation.mapgpt.core.api

interface MapGptContextProvider {
    fun getContext(): MapGptContextDTO?
}

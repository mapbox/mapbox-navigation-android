package com.mapbox.navigation.ui.maps.route.routearrow.api

interface RouteArrowResourceProvider {
    fun getArrowBorderColor(): Int
    fun getArrowColor(): Int
    fun getArrowHeadIcon(): Int
    fun getArrowHeadCasingIcon(): Int
}

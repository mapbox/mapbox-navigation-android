package com.mapbox.navigation.base.trip.model.roadobject.reststop

import androidx.annotation.StringDef

/**
 * Types of [Amenity]
 */
object AmenityType {
    /**
     * Amenity type undefined
     */
    const val UNDEFINED: String = "undefined"

    /**
     * Amenity type gas station
     */
    const val GAS_STATION: String = "gas_station"

    /**
     * Amenity type electric charging station
     */
    const val ELECTRIC_CHARGING_STATION: String = "electric_charging_station"

    /**
     * Amenity type toilet
     */
    const val TOILET: String = "toilet"

    /**
     * Amenity type coffee
     */
    const val COFFEE: String = "coffee"

    /**
     * Amenity type restaurant
     */
    const val RESTAURANT: String = "restaurant"

    /**
     * Amenity type snack
     */
    const val SNACK: String = "snack"

    /**
     * Amenity type atm
     */
    const val ATM: String = "atm"

    /**
     * Amenity type info
     */
    const val INFO: String = "info"

    /**
     * Amenity type baby care
     */
    const val BABY_CARE: String = "baby_care"

    /**
     * Amenity type facilities for disabled
     */
    const val FACILITIES_FOR_DISABLED: String = "facilities_for_disabled"

    /**
     * Amenity type shop
     */
    const val SHOP: String = "shop"

    /**
     * Amenity type telephone
     */
    const val TELEPHONE: String = "telephone"

    /**
     * Amenity type hotel
     */
    const val HOTEL: String = "hotel"

    /**
     * Amenity type hotspring
     */
    const val HOTSPRING: String = "hotspring"

    /**
     * Amenity type shower
     */
    const val SHOWER: String = "shower"

    /**
     * Amenity type picnic shelter
     */
    const val PICNIC_SHELTER: String = "picnic_shelter"

    /**
     * Amenity type post
     */
    const val POST: String = "post"

    /**
     * Amenity type fax
     */
    const val FAX: String = "fax"

    /**
     * Amenity type.
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        UNDEFINED,
        GAS_STATION,
        ELECTRIC_CHARGING_STATION,
        TOILET,
        COFFEE,
        RESTAURANT,
        SNACK,
        ATM,
        INFO,
        BABY_CARE,
        FACILITIES_FOR_DISABLED,
        SHOP,
        TELEPHONE,
        HOTEL,
        HOTSPRING,
        SHOWER,
        PICNIC_SHELTER,
        POST,
        FAX,
    )
    annotation class Type
}

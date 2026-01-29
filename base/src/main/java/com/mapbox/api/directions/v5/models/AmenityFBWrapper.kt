package com.mapbox.api.directions.v5.models

import com.google.flatbuffers.FlexBuffers
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.DirectionsCriteria.AmenityTypeCriteria
import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.unhandledEnumMapping
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class AmenityFBWrapper private constructor(
    private val fb: FBAmenity,
) : Amenity(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun type(): String = fb.type.fbAmenityTypeCriteria("type", unrecognizeFlexBufferMap)

    override fun name(): String? = fb.name

    override fun brand(): String? = fb.brand

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("Amenity#toBuilder()")
    }

    internal companion object {

        internal fun wrap(fb: FBAmenity?): Amenity? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> AmenityFBWrapper(fb)
            }
        }

        @AmenityTypeCriteria
        private fun Byte.fbAmenityTypeCriteria(
            propertyName: String,
            unrecognized: FlexBuffers.Map?,
        ): String {
            return when (this) {
                FBAmenityType.GasStation -> DirectionsCriteria.AMENITY_TYPE_GAS_STATION
                FBAmenityType.ElectricChargingStation ->
                    DirectionsCriteria.AMENITY_TYPE_ELECTRIC_CHARGING_STATION
                FBAmenityType.Toilet -> DirectionsCriteria.AMENITY_TYPE_TOILET
                FBAmenityType.Coffee -> DirectionsCriteria.AMENITY_TYPE_COFFEE
                FBAmenityType.Restaurant -> DirectionsCriteria.AMENITY_TYPE_RESTAURANT
                FBAmenityType.Snack -> DirectionsCriteria.AMENITY_TYPE_SNACK
                FBAmenityType.Atm -> DirectionsCriteria.AMENITY_TYPE_ATM
                FBAmenityType.Info -> DirectionsCriteria.AMENITY_TYPE_INFO
                FBAmenityType.BabyCare -> DirectionsCriteria.AMENITY_TYPE_BABY_CARE
                FBAmenityType.FacilitiesForDisabled ->
                    DirectionsCriteria.AMENITY_TYPE_FACILITIES_FOR_DISABLED
                FBAmenityType.Shop -> DirectionsCriteria.AMENITY_TYPE_SHOP
                FBAmenityType.Telephone -> DirectionsCriteria.AMENITY_TYPE_TELEPHONE
                FBAmenityType.Hotel -> DirectionsCriteria.AMENITY_TYPE_HOTEL
                FBAmenityType.Hotspring -> DirectionsCriteria.AMENITY_TYPE_HOTSPRING
                FBAmenityType.Shower -> DirectionsCriteria.AMENITY_TYPE_SHOWER
                FBAmenityType.PicnicShelter -> DirectionsCriteria.AMENITY_TYPE_PICNIC_SHELTER
                FBAmenityType.Post -> DirectionsCriteria.AMENITY_TYPE_POST
                FBAmenityType.Fax -> DirectionsCriteria.AMENITY_TYPE_FAX
                FBAmenityType.Unknown -> unrecognized?.get(propertyName)?.asString()
                    ?: throw IllegalStateException(
                        "$propertyName is Unknown in fb, but missing in unrecognized map",
                    )
                else -> unhandledEnumMapping(propertyName, this)
            }
        }
    }
}

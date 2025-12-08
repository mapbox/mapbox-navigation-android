package com.mapbox.api.directions.v5.models.utils

internal fun unhandledEnumMapping(
    propertyName: String,
    enumValue: Byte?,
): Nothing {
    throw UnhandedEnumMapping(propertyName, enumValue)
}

internal class UnhandedEnumMapping(
    propertyName: String,
    enumValue: Byte?,
) : Throwable("Unhandled enum value $enumValue for property '$propertyName'")

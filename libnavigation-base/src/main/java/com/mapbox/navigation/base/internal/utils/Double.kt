package com.mapbox.navigation.base.internal.utils

// https://jqno.nl/equalsverifier/errormessages/double-equals-doesnt-use-doublecompare-for-field-foo/
fun Double?.safeCompareTo(other: Double?): Boolean {
    return when {
        this == null -> other == null
        other == null -> false
        else -> compareTo(other) == 0
    }
}

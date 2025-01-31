package com.mapbox.navigation.base.internal.utils

// https://jqno.nl/equalsverifier/errormessages/float-equals-doesnt-use-floatcompare-for-field-foo/
fun Float?.safeCompareTo(other: Float?): Boolean {
    return when {
        this == null -> other == null
        other == null -> false
        else -> compareTo(other) == 0
    }
}

package com.mapbox.navigation.testing.utils.nro

import com.mapbox.navigation.base.BuildConfig.NATIVE_ROUTE_OBJECT_DEFAULT
import org.junit.Assume.assumeFalse

private fun assumeNotNRO(reason: String) {
    assumeFalse(
        "test is disabled as NRO is enabled while: $reason",
        NATIVE_ROUTE_OBJECT_DEFAULT,
    )
}

fun assumeNotNROBecauseOfAlternativesDropDuringSerialization() {
    assumeNotNRO("With NRO Nav SDK doesn't have to drop alternatives to parse routes")
}

fun assumeNotNROBecauseOfClientSideUpdate() {
    assumeNotNRO("NRO doesn't support client-side route updates")
}

fun assumeNotNROBecauseOfSerialization() {
    // TODO: https://mapbox.atlassian.net/browse/NAVAND-6775
    assumeNotNRO("NRO doesn't support serialization")
}

fun assumeNotNROBecauseToBuilderIsRequiredForTest() {
    assumeNotNRO(
        "NRO doesn't let customers to create new models from existing" +
            " by calling toBuilder()",
    )
}

fun assumeNotNROBecauseEmptyRefreshTllBreaksExpirationTime() {
    assumeNotNRO(
        "NRO doesn't handle empty refresh ttl well" +
        ": https://mapbox.atlassian.net/browse/NAVAND-6952",
    )
}

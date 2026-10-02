package com.mapbox.navigation.ui.androidauto.deeplink

import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.core.geodeeplink.GeoDeeplinkParser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * This class is responsible for converting external geo deeplinks into internal domain objects.
 *
 * Public documentation for the geo deeplink can be found here
 * https://developers.google.com/maps/documentation/urls/android-intents
 */
object GeoDeeplinkParser {

    private val destinationChannel = Channel<GeoDeeplink>(Channel.CONFLATED)
    val destinationReceiveChannel: ReceiveChannel<GeoDeeplink> = destinationChannel

    fun parseAndSave(geoDeeplink: String?) {
        GeoDeeplinkParser.parse(geoDeeplink)?.let { destinationChannel.trySend(it) }
    }
}

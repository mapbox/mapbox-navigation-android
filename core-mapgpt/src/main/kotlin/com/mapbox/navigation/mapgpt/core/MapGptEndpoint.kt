package com.mapbox.navigation.mapgpt.core

/**
 * Wrapper that defines the properties of endpoints required to be used with MapGPT.
 *
 * @param name describes the flavor of endpoint
 * @param streamingApiHost host for streaming data
 * @param conversationApiHost host for sending queries
 */
class MapGptEndpoint private constructor(
    val name: String,
    val streamingApiHost: String,
    val streamingAsrApiHost: String,
    val conversationApiHost: String,
) {

    override fun toString(): String {
        return "MapGptEndpoint(name=$name, streamingApiHost=$streamingApiHost, conversationApiHost=$conversationApiHost)"
    }

    companion object {

        /**
         * Creates a new instance of [MapGptEndpoint] with the parameters supplied.
         *
         * @param name describes the kind of endpoint
         * @param streamingApiHost api host to stream to
         * @param conversationApiHost api host conversation should be posted to
         */
        fun create(
            name: String = "production",
            streamingApiHost: String = "wss://mapgpt-production-ws.mapbox.com",
            streamingAsrApiHost: String = "wss://api-navgptasr-production.mapbox.com",
            conversationApiHost: String = "mapgpt-production-api.mapbox.com",
        ): MapGptEndpoint {
            return MapGptEndpoint(
                name = name,
                streamingApiHost = streamingApiHost,
                streamingAsrApiHost = streamingAsrApiHost,
                conversationApiHost = conversationApiHost,
            )
        }
    }
}

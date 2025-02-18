package com.mapbox.navigation.mapgpt.core.api

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Definition of a single UI component to be rendered on the card.
 */
@Serializable
abstract class CardComponent {

    /**
     * Card Component of type [Text]
     *
     * @param content to be displayed on the card
     * @param fontSize of the [Text]
     * @param fontColor of the [Text]
     */
    @Serializable
    @SerialName("text")
    data class Text(
        val content: String,
        @SerialName("font_size")
        val fontSize: String,
        @SerialName("font_color")
        val fontColor: String,
    ) : CardComponent()

    /**
     * Card component of type [Stack]. Stack is always vertical with 2 rows: top_row and bottom_row
     * which in turn is a nested [CardComponent] that can either be of other types.
     *
     * @param topRow [CardComponent] type that can either be a [Text], [Image], [Spacer] or [Stack]
     * @param bottomRow [CardComponent] type that can either be a [Text], [Image], [Spacer] or [Stack]
     */
    @Serializable
    @SerialName("stack")
    data class Stack(
        @SerialName("top_row")
        val topRow: List<CardComponent>?,
        @SerialName("bottom_row")
        val bottomRow: List<CardComponent>?,
    ) : CardComponent()

    /**
     * Card Component of type [Spacer] which denotes empty space between two other components.
     */
    @Serializable
    @SerialName("spacer")
    class Spacer : CardComponent()

    /**
     * Card Component of type [Image] which describes the image uri for light and dark mode. This
     * should be used to display a place such as a restaurant or an icon
     *
     * @param uriLight image/icon to be displayed in light mode
     * @param uriDark image/icon to be displayed in dark mode
     * @param aspectRatio indicates UI element is a square
     */
    @Serializable
    @SerialName("image")
    data class Image(
        @SerialName("uri_light")
        val uriLight: String,
        @SerialName("uri_dark")
        val uriDark: String?,
        @SerialName("aspect_ratio")
        val aspectRatio: Float?,
    ) : CardComponent()

    /**
     * Payload associated with unknown types.
     *
     * @param details map of key value pairs
     */
    @Serializable
    data class Unknown(
        val details: Map<String, JsonElement>,
    ) : CardComponent() {

        internal object UnknownDeserializationStrategy :
            DeserializationStrategy<Unknown> {

            override val descriptor: SerialDescriptor =
                buildClassSerialDescriptor("Unknown") {
                    element<JsonObject>("details")
                }

            override fun deserialize(decoder: Decoder): Unknown {
                val jsonInput = decoder as? JsonDecoder
                    ?: error("Can be deserialized only by JSON")
                val json = jsonInput.decodeJsonElement().jsonObject
                val details = json.toMutableMap()
                return Unknown(
                    details = details,
                )
            }
        }
    }
}

/**
 * Payload associated with the card of type entity.
 */
@Serializable
abstract class CardPayload {

    /**
     * Payload associated with the action play music
     *
     * @param spotifyUri URI of the song to play
     * @param provider music player provider defined by MusicPlayerProvider
     * @param uri uri of the track to be played
     * @param song name of the song if available, null otherwise
     * @param artist name of the artist if available, null otherwise
     */
    @Serializable
    @SerialName("mbx.music")
    data class PlayMusic(
        @SerialName("spotify_uri")
        val spotifyUri: String? = null,
        @SerialName("provider")
        val provider: String? = null,
        @SerialName("uri")
        val uri: String? = null,
        @SerialName("song")
        val song: String? = null,
        @SerialName("artist")
        val artist: String? = null,
    ) : CardPayload()

    /**
     * Payload associated with the action POI
     *
     * @param geocoded reverse geocoded place information
     */
    @Serializable
    @SerialName("mbx.poi")
    data class Poi(
        val geocoded: SessionFrame.Geocoded,
    ) : CardPayload()

    /**
     * Payload associated with unknown types.
     *
     * @param details map of key value pairs
     */
    @Serializable
    data class Unknown(
        val details: Map<String, JsonElement>,
    ) : CardPayload() {

        internal object UnknownDeserializationStrategy :
            DeserializationStrategy<Unknown> {

            override val descriptor: SerialDescriptor =
                buildClassSerialDescriptor("Unknown") {
                    element<JsonObject>("details")
                }

            override fun deserialize(decoder: Decoder): Unknown {
                val jsonInput = decoder as? JsonDecoder
                    ?: error("Can be deserialized only by JSON")
                val json = jsonInput.decodeJsonElement().jsonObject
                val details = json.toMutableMap()
                return Unknown(
                    details = details,
                )
            }
        }
    }
}

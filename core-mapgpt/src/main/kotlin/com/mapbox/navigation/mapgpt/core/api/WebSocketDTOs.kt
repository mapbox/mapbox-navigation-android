@file:OptIn(ExperimentalSerializationApi::class)

package com.mapbox.navigation.mapgpt.core.api

import com.mapbox.navigation.mapgpt.core.api.autopilot.AutopilotCarSpeed
import com.mapbox.navigation.mapgpt.core.api.autopilot.AutopilotDrivingStyle
import com.mapbox.navigation.mapgpt.core.api.autopilot.AutopilotLaneDirection
import com.mapbox.navigation.mapgpt.core.api.camera.CameraTracking
import com.mapbox.navigation.mapgpt.core.api.climate.Occupant
import com.mapbox.navigation.mapgpt.core.api.map.MapLighting
import com.mapbox.navigation.mapgpt.core.api.map.MapTheme
import com.mapbox.navigation.mapgpt.core.api.window.WindowPosition
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val sessionFrameSerializerModule = SerializersModule {

    // TODO add correct round-tripping of unknown polymorphic types,
    //  right now the unknown types encode non-abstract fields in a separate 'details' JSON object
    //  when they are serialized back to JSON

    polymorphic(baseClass = SessionFrame::class) {
        subclass(SessionFrame.SendEvent::class)
        subclass(SessionFrame.StartSession::class)
        subclass(SessionFrame.Unknown::class)
        defaultDeserializer {
            SessionFrame.Unknown.UnknownDeserializationStrategy
        }
    }
    polymorphic(baseClass = SessionFrame.SendEvent.Body::class) {
        subclass(SessionFrame.SendEvent.Body.Conversation::class)
        subclass(SessionFrame.SendEvent.Body.Entity::class)
        subclass(SessionFrame.SendEvent.Body.TableReservationStatus::class)
        subclass(SessionFrame.SendEvent.Body.PlayMusic::class)
        subclass(SessionFrame.SendEvent.Body.PauseMusic::class)
        subclass(SessionFrame.SendEvent.Body.ResumeMusic::class)
        subclass(SessionFrame.SendEvent.Body.StartNavigation::class)
        subclass(SessionFrame.SendEvent.Body.StopNavigation::class)
        subclass(SessionFrame.SendEvent.Body.AddWaypoint::class)
        subclass(SessionFrame.SendEvent.Body.UpdateMapStyle::class)
        subclass(SessionFrame.SendEvent.Body.UpdateCameraTracking::class)
        subclass(SessionFrame.SendEvent.Body.AutopilotSetSpeed::class)
        subclass(SessionFrame.SendEvent.Body.AutopilotAdjustSpeed::class)
        subclass(SessionFrame.SendEvent.Body.AutopilotSetDrivingStyle::class)
        subclass(SessionFrame.SendEvent.Body.AutopilotChangeLane::class)
        subclass(SessionFrame.SendEvent.Body.ClimateSetAc::class)
        subclass(SessionFrame.SendEvent.Body.ClimateSetAuto::class)
        subclass(SessionFrame.SendEvent.Body.ClimateSetDefog::class)
        subclass(SessionFrame.SendEvent.Body.ClimateSetDefrost::class)
        subclass(SessionFrame.SendEvent.Body.ClimateSetTemperature::class)
        subclass(SessionFrame.SendEvent.Body.SetWindow::class)
        subclass(SessionFrame.SendEvent.Body.LockWindow::class)
        subclass(SessionFrame.SendEvent.Body.NoResponse::class)
        subclass(SessionFrame.SendEvent.Body.StopListening::class)
        subclass(SessionFrame.SendEvent.Body.Landmark::class)
        subclass(SessionFrame.SendEvent.Body.PhoneCall::class)
        subclass(SessionFrame.SendEvent.Body.Unknown::class)
        defaultDeserializer {
            SessionFrame.SendEvent.Body.Unknown.UnknownDeserializationStrategy
        }
    }
    polymorphic(baseClass = SessionFrame.SendEvent.Body.Entity.Data::class) {
        subclass(SessionFrame.SendEvent.Body.Entity.Data.Place::class)
        subclass(SessionFrame.SendEvent.Body.Entity.Data.Card::class)
        subclass(SessionFrame.SendEvent.Body.Entity.Data.Unknown::class)
        defaultDeserializer {
            SessionFrame.SendEvent.Body.Entity.Data.Unknown.UnknownDeserializationStrategy
        }
    }
    polymorphic(baseClass = CardComponent::class) {
        subclass(CardComponent.Text::class)
        subclass(CardComponent.Stack::class)
        subclass(CardComponent.Spacer::class)
        subclass(CardComponent.Image::class)
        subclass(CardComponent.Unknown::class)
        defaultDeserializer {
            CardComponent.Unknown.UnknownDeserializationStrategy
        }
    }
    polymorphic(baseClass = CardPayload::class) {
        subclass(CardPayload.PlayMusic::class)
        subclass(CardPayload.Poi::class)
        defaultDeserializer {
            CardPayload.Unknown.UnknownDeserializationStrategy
        }
    }
    polymorphic(baseClass = OutgoingSessionFrame::class) {
        subclass(OutgoingSessionFrame.GetSessionId::class)
    }
}

@OptIn(ExperimentalSerializationApi::class)
private val jsonSerialization = Json {
    serializersModule = sessionFrameSerializerModule
    explicitNulls = false
    ignoreUnknownKeys = true
}

/**
 * Definition of a frame that carries information across the communication channel with MapGPT backend.
 */
@Serializable
@JsonClassDiscriminator(discriminator = "action")
abstract class SessionFrame {

    /**
     * Definition of an event that is generated by the server when certain things happen, such as extracting a POI from a user / assistant conversation.
     *
     * @param body contents of the event
     */
    @Serializable
    @SerialName("send-event")
    data class SendEvent(
        val body: Body,
    ) : SessionFrame() {

        /**
         * Defines the event specifics, which varies based on the event type.
         */
        @Serializable
        abstract class Body {

            /**
             * A unique increasing ID field for the event. This can be used by the client to reference the last received event ID when reconnecting to the service.
             *
             * @see [MapGptService.connect]
             */
            abstract val id: Long

            /**
             * Creation type of the event as a unix timestamp
             */
            abstract val timestamp: Long

            /**
             * Describes which conversational chunk the event is tied to, in format of `{date}@{id}`
             * where the [chunkPrefix] should be considered as an ID of the AI response
             * and [chunkOffset] is the logical order in the response stream.
             */
            @SerialName("chunk_id")
            abstract val chunkId: String

            /**
             * Represents an ID of the AI response.
             *
             * See [chunkId].
             */
            val chunkPrefix: String by lazy { chunkId.split("@").first() }

            /**
             * Represents logical order in the response stream.
             *
             * See [chunkId].
             */
            val chunkOffset: Int by lazy { chunkId.split("@")[1].toInt() }

            /**
             * Defines events that were not generated in direct response to user query
             * but retroactively or spontaneously by the assistant.
             */
            @SerialName("is_supplement")
            abstract val isSupplement: Boolean

            /**
             * Defines a conversation chunk. A single chunk is typically a sentence in a longer AI response.
             *
             * Conversation can be in direct response to something the user said or may be sent separately, e.g., after processing a restaurant reservation request.
             *
             * @param data conversation contents
             */
            @Serializable
            @SerialName("conversation")
            data class Conversation(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {

                /**
                 * Conversation contents.
                 *
                 * @param conversationId a unique string for this conversation
                 * @param content the sentence in a response
                 * @param initial whether this is the first chunk in the response stream
                 * @param confirmation whether this chunk is a confirmation of a user action
                 * @param final whether this is the last chunk in the response stream
                 * @param maxTokens whether conversation has hit the profile's response token limit
                 */
                @Serializable
                data class Data(
                    @SerialName("conversation_id")
                    val conversationId: String,
                    val content: String,
                    val initial: Boolean = false,
                    val confirmation: Boolean = false,
                    val final: Boolean = false,
                    @SerialName("max_tokens")
                    val maxTokens: Boolean = false,
                )
            }

            /**
             * Defines entities, such as POIs or song names, extracted from a conversation or otherwise generated using this format.
             *
             * @param data entity contents
             */
            @Serializable
            @SerialName("entity")
            data class Entity(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: List<Data>,
            ) : Body() {

                /**
                 * Entity contents.
                 */
                @Serializable
                abstract class Data {

                    /**
                     * Defines a POI entity.
                     *
                     * @param name an identified name of the entity such as restaurant name
                     * @param geocoded reverse geocoded place information
                     */
                    @Serializable
                    @SerialName("place")
                    data class Place(
                        val name: String,
                        @SerialName("geocoded") val geocoded: Geocoded? = null,
                    ) : Data()

                    /**
                     * Defines a Card entity.
                     *
                     * @param components list of [CardComponent] describing the UI
                     * @param anchoredComponent [CardComponent] of the type [CardComponent.Image]
                     * describing the light and dark uri for the data provider if applicable
                     * @param payload additional payload associated with the card that will be sent
                     * as a callback upon interaction.
                     */
                    @Serializable
                    @SerialName("card")
                    data class Card(
                        val components: List<CardComponent>,
                        @SerialName("anchored_component")
                        val anchoredComponent: CardComponent?,
                        val payload: CardPayload?,
                    ) : Data()

                    /**
                     * Payload associated with unknown types.
                     *
                     * @param details map of key value pairs
                     */
                    @Serializable
                    data class Unknown(
                        val details: Map<String, JsonElement>,
                    ) : Data() {

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
            }

            /**
             * Defines reservation action.
             *
             * @param data reservation contents
             */
            @Serializable
            @SerialName("make_table_reservation_status")
            data class TableReservationStatus(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {

                /**
                 * Reservation contents.
                 *
                 * @param success whether reservation has been successful
                 * @param location name of the reservation place
                 * @param time data/time in, for example `2023-07-19T19:00:00`
                 * @param partySize number of people
                 * @param provider service provider through which the reservation has been done
                 * @param geocoded reverse geocoded place information
                 */
                @Serializable
                data class Data(
                    val success: Boolean,
                    val location: String,
                    val time: String,
                    @SerialName("party_size")
                    val partySize: Int,
                    val provider: String,
                    @SerialName("geocoded") val geocoded: Geocoded? = null,
                )
            }

            /**
             * Defines action to play a song.
             *
             * @param data play music contents
             */
            @Serializable
            @SerialName("play_music")
            data class PlayMusic(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {

                /**
                 * Play music contents.
                 *
                 * @param provider music provider key
                 * @param uri provider specific uri of the track to be played
                 * @param song name of the song
                 * @param artist name of the artist
                 */
                @Serializable
                data class Data(
                    @SerialName("provider")
                    val provider: String? = null,
                    @SerialName("uri")
                    val uri: String? = null,
                    @SerialName("song")
                    val song: String? = null,
                    @SerialName("artist")
                    val artist: String? = null,
                )
            }

            /**
             * Defines action to pause music.
             */
            @Serializable
            @SerialName("pause_music")
            data class PauseMusic(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
            ) : Body()

            /**
             * Defines action to resume music.
             */
            @Serializable
            @SerialName("resume_music")
            data class ResumeMusic(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
            ) : Body()

            /**
             * Defines an action to turn the car AC on/off
             *
             * @param data set ac content
             */
            @Serializable
            @SerialName("control_climate.set_ac")
            data class ClimateSetAc(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ): Body() {

                /**
                 * Car AC turn on/off content.
                 * @param value denotes car AC is on if set to true, off otherwise
                 */
                @Serializable
                data class Data(
                    val value: Boolean,
                )
            }

            /**
             * Defines an action to turn the car auto climate on/off
             *
             * @param data set hvac auto mode content
             */
            @Serializable
            @SerialName("control_climate.set_auto")
            data class ClimateSetAuto(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ): Body() {

                /**
                 * Car auto climate turn on/off content.
                 * @param value denotes car auto climate is on if set to true, off otherwise
                 */
                @Serializable
                data class Data(
                    val value: Boolean,
                )
            }

            /**
             * Defines an action to turn the car defogger on/off
             *
             * @param data set hvac defog content
             */
            @Serializable
            @SerialName("control_climate.set_defog")
            data class ClimateSetDefog(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ): Body() {

                /**
                 * Car defogger turn on/off content.
                 * @param value denotes car defogger is on if set to true, off otherwise
                 */
                @Serializable
                data class Data(
                    val value: Boolean,
                )
            }

            /**
             * Defines an action to turn the car defroster on/off
             *
             * @param data set hvac defrost content
             */
            @Serializable
            @SerialName("control_climate.set_defrost")
            data class ClimateSetDefrost(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ): Body() {

                /**
                 * Car defroster turn on/off content.
                 * @param value denotes car defroster is on if set to true, off otherwise
                 */
                @Serializable
                data class Data(
                    val value: Boolean,
                )
            }

            /**
             * Defines an action to set the temperature of the car
             *
             * @param data set hvac temperature content
             */
            @Serializable
            @SerialName("control_climate.set_temperature")
            data class ClimateSetTemperature(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ): Body() {

                /**
                 * Car set temperature content.
                 * @param temperature denotes the car temperature in either C/F based on the unit set
                 * to [MapGptAppContextDTO.temperatureUnits]
                 * @param occupant denotes the [Occupant] for which the car temperature has been set.
                 */
                @Serializable
                data class Data(
                    val temperature: Float,
                    val occupant: Occupant,
                )
            }

            /**
             * Defines an action to set the window position of the car
             *
             * @param data set vehicle window content
             */
            @Serializable
            @SerialName("control_windows.set_window")
            data class SetWindow(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ): Body() {

                /**
                 * Car set temperature content.
                 * @param position denotes the position of the window to set to.
                 * @param occupant denotes the [Occupant] for which the window [position] has to be set.
                 */
                @Serializable
                data class Data(
                    val position: WindowPosition,
                    val occupant: Occupant,
                )
            }

            /**
             * Defines an action to lock/unlock the windows of the car
             *
             * @param data set vehicle lock/unlock window content
             */
            @Serializable
            @SerialName("control_windows.lock_window")
            data class LockWindow(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ): Body() {

                /**
                 * Car set temperature content.
                 * @param value the windows are locked if true; unlocked otherwise.
                 */
                @Serializable
                data class Data(
                    val value: Boolean,
                )
            }

            /**
             * Defines action to start navigation.
             *
             * @param data start navigation contents
             */
            @Serializable
            @SerialName("start_navigation")
            data class StartNavigation(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {

                /**
                 * Start navigation contents.
                 *
                 * @param location name of the place
                 * @param geocoded geocoded feature of the location
                 * @param favorite key to identify the location if it is a favorite
                 */
                @Serializable
                data class Data(
                    val location: String? = null,
                    val geocoded: Geocoded? = null,
                    val favorite: String? = null,
                )
            }

            /**
             * Defines action to add a waypoint to an existing route.
             *
             * @param data add waypoint contents
             */
            @Serializable
            @SerialName("add_waypoint")
            data class AddWaypoint(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {

                /**
                 * Add Waypoint contents.
                 *
                 * @param index at which the waypoint is added
                 * @param location name of the place
                 * @param geocoded geocoded feature of the location
                 */
                @Serializable
                data class Data(
                    val index: Int,
                    val location: String,
                    @SerialName("geocoded") val geocoded: Geocoded? = null,
                )
            }

            /**
             * Defines action to stop navigation.
             */
            @Serializable
            @SerialName("stop_navigation")
            data class StopNavigation(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
            ) : Body()

            /**
             * Defines action to set speed using autopilot controls.
             *
             * @param data add autopilot contents
             */
            @Serializable
            @SerialName("control_autopilot.set_speed")
            data class AutopilotSetSpeed(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {

                /**
                 * Add adjust speed contents.
                 *
                 * @param speed indicates the speed to be set
                 */
                @Serializable
                data class Data(
                    @SerialName("speed")
                    val speed: Int,
                )
            }

            /**
             * Defines action to adjust speed using autopilot controls.
             *
             * @param data add autopilot contents
             */
            @Serializable
            @SerialName("control_autopilot.adjust_speed")
            data class AutopilotAdjustSpeed(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {

                /**
                 * Add adjust speed contents.
                 *
                 * @param carSpeed indicates whether the speed should be
                 * - [AutopilotCarSpeed.ACCELERATE]
                 * - [AutopilotCarSpeed.DECELERATE]
                 * - [AutopilotCarSpeed.UNKNOWN]
                 * @param delta a specific value by which the speed should accelerate or decelerate.
                 * If a specific value is not provided by the end user, the value will be null.
                 */
                @Serializable
                data class Data(
                    @SerialName("value")
                    val carSpeed: AutopilotCarSpeed,
                    @SerialName("delta")
                    val delta: Int?,
                )
            }

            /**
             * Defines action to set driving style using autopilot controls.
             *
             * @param data add autopilot contents
             */
            @Serializable
            @SerialName("control_autopilot.set_drive_style")
            data class AutopilotSetDrivingStyle(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {

                /**
                 * Add driving style contents.
                 *
                 * @param drivingStyle set driving style to one of
                 * - [AutopilotDrivingStyle.AGGRESSIVE]
                 * - [AutopilotDrivingStyle.STANDARD]
                 * - [AutopilotDrivingStyle.RELAXED]
                 * - [AutopilotDrivingStyle.UNKNOWN]
                 */
                @Serializable
                data class Data(
                    @SerialName("value")
                    val drivingStyle: AutopilotDrivingStyle,
                )
            }

            /**
             * Defines action to change lanes using autopilot controls.
             *
             * @param data add autopilot contents
             */
            @Serializable
            @SerialName("control_autopilot.change_lane")
            data class AutopilotChangeLane(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {

                /**
                 * Add lane change contents.
                 *
                 * @param laneDirection direction of the lane change to either
                 * - [AutopilotLaneDirection.LEFT]
                 * - [AutopilotLaneDirection.RIGHT]
                 * - [AutopilotLaneDirection.UNKNOWN]
                 * @param laneCount count by which to change lanes.
                 */
                @Serializable
                data class Data(
                    @SerialName("value")
                    val laneDirection: AutopilotLaneDirection,
                    @SerialName("laneCount")
                    val laneCount: Int,
                )
            }

            /**
             * Defines action containing no verbal response for the user query.
             */
            @Serializable
            @SerialName("no_response")
            data class NoResponse(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
            ) : Body()

            /**
             * Defines action ordering MapGPT to close the microphone and stop capturing input.
             */
            @Serializable
            @SerialName("stop_listening")
            data class StopListening(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
            ) : Body()

            /**
             * Defines action ordering MapGPT to change the tracking state of camera associated with the map.
             */
            @Serializable
            @SerialName("update_map_style")
            data class UpdateMapStyle(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {

                /**
                 * Add camera tracking contents.
                 *
                 * @param theme map theme
                 * - [MapTheme.DEFAULT]
                 * - [MapTheme.FADED]
                 * - [MapTheme.MONO]
                 * - [MapTheme.UNKNOWN]
                 * @param lighting map lighting
                 * - [MapLighting.AUTO]
                 * - [MapLighting.DAWN]
                 * - [MapLighting.DAY]
                 * - [MapLighting.DUSK]
                 * - [MapLighting.NIGHT]
                 * - [MapLighting.UNKNOWN]
                 */
                @Serializable
                data class Data(
                    @SerialName("theme")
                    val theme: MapTheme? = null,
                    @SerialName("lighting")
                    val lighting: MapLighting? = null,
                )
            }

            /**
             * Defines action ordering MapGPT to change the tracking state of camera associated with the map.
             */
            @Serializable
            @SerialName("camera_tracking")
            data class UpdateCameraTracking(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {

                /**
                 * Add camera tracking contents.
                 *
                 * @param cameraTracking tracking state of the camera
                 * - [CameraTracking.RECENTER]
                 * - [CameraTracking.TRACKING_PUCK]
                 * - [CameraTracking.TRACKING_NORTH]
                 * - [CameraTracking.ROUTE_OVERVIEW]
                 * - [CameraTracking.UNKNOWN]
                 */
                @Serializable
                data class Data(
                    @SerialName("value")
                    val cameraTracking: CameraTracking,
                )
            }

            /**
             * Defines action related to landmark.
             */
            @Serializable
            @SerialName("landmark")
            data class Landmark(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {

                /**
                 * Add landmark contents.
                 *
                 * @param place geocoded information about the landmark.
                 */
                @Serializable
                data class Data(
                    val place: Entity.Data.Place,
                )
            }

            @Serializable
            @SerialName("phone_call")
            data class PhoneCall(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val data: Data,
            ) : Body() {
                @Serializable
                data class Data(
                    @SerialName("phone_number")
                    val phoneNumber: String,
                )
            }

            /**
             * Payload associated with unknown types.
             *
             * @param details map of key value pairs
             */
            @Serializable
            data class Unknown(
                override val id: Long,
                override val timestamp: Long,
                @SerialName("chunk_id")
                override val chunkId: String,
                @SerialName("is_supplement")
                override val isSupplement: Boolean = false,
                val details: Map<String, JsonElement>,
            ) : Body() {

                internal object UnknownDeserializationStrategy : DeserializationStrategy<Unknown> {

                    override val descriptor: SerialDescriptor =
                        buildClassSerialDescriptor("Unknown") {
                            element<Long>("id")
                            element<Long>("timestamp")
                            element<String>("chunk_id")
                            element<Boolean>("is_supplement")
                            element<JsonObject>("details")
                        }

                    override fun deserialize(decoder: Decoder): Unknown {
                        val jsonInput = decoder as? JsonDecoder
                            ?: error("Can be deserialized only by JSON")
                        val json = jsonInput.decodeJsonElement().jsonObject
                        val id = json.getValue("id").jsonPrimitive.content.toLong()
                        val timestamp = json.getValue("timestamp").jsonPrimitive.content.toLong()
                        val chunkId = json.getValue("chunk_id").jsonPrimitive.content
                        val isSupplement = json["is_supplement"]
                            ?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
                        val details = json.toMutableMap()
                        details.remove("id")
                        details.remove("timestamp")
                        details.remove("chunk_id")
                        details.remove("is_supplement")
                        return Unknown(
                            id = id,
                            timestamp = timestamp,
                            chunkId = chunkId,
                            isSupplement = isSupplement,
                            details = details,
                        )
                    }
                }
            }
        }
    }

    /**
     * Called when a connection with the backend service is established and a new session is started.
     *
     * @param body content associated with the session
     */
    @Serializable
    @SerialName("start-session")
    data class StartSession(
        val body: Body,
    ) : SessionFrame() {

        /**
         * Content associated with the session
         *
         * @param sessionId session id
         */
        @Serializable
        data class Body(
            @SerialName("session_id")
            val sessionId: String,
        )
    }

    /**
     * Payload associated with unknown types.
     *
     * @param details map of key value pairs
     */
    @Serializable
    data class Unknown(
        val details: Map<String, JsonElement>,
    ) : SessionFrame() {

        internal object UnknownDeserializationStrategy : DeserializationStrategy<Unknown> {

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

    /**
     * Object detailing forward geocoding information
     *
     * @param type
     * @param features list of features that could be queried on a rendered map
     */
    @Serializable
    data class Geocoded(
        @SerialName("type") val type: String? = null,
        @SerialName("features") val features: List<Features> = emptyList(),
    ) {

        /**
         * Informs about the rendered features that could be queried
         *
         * @param type
         * @param geometry defines the position of the object in query
         * @param properties details specific to a queried [Features]
         */
        @Serializable
        data class Features(
            @SerialName("type") val type: String? = null,
            @SerialName("geometry") val geometry: Geometry?,
            @SerialName("properties") val properties: Properties? = null,
        ) {

            /**
             * Defines the position of the queried [Features].
             *
             * @param coordinates position in long/lat
             * @param type
             */
            @Serializable
            data class Geometry(
                @SerialName("coordinates") val coordinates: List<Double> = listOf(),
                @SerialName("type") val type: String? = null,
            )

            /**
             * Specific information about each feature queried.
             *
             * @param description description of the feature
             * @param id id of the feature
             * @param placeType list of placeType of the feature
             * @param context list of context of the feature
             */
            @Serializable
            data class Properties(
                @SerialName("description") val description: String? = null,
                @SerialName("id") val id: String? = null,
                @SerialName("place_type") val placeType: List<String> = listOf(),
                @SerialName("context") val context: List<Context> = listOf(),
                @SerialName("maki") val maki: String? = null,
                @SerialName("poi_category") val poiCategory: List<String> = listOf(),
                @SerialName("metadata") val metadata: Metadata? = null,
                @SerialName("routable_points") val routablePoints: List<RoutablePoints> = listOf(),
                @SerialName("feature_name") val featureName: String? = null,
                @SerialName("matching_name") val matchingName: String? = null,
            ) {

                @Serializable
                data class RoutablePoints(
                    @SerialName("name") val name: String? = null,
                    @SerialName("coordinates") val coordinates: List<Double> = listOf(),
                )

                @Serializable
                data class Context(
                    @SerialName("layer") val layer: String? = null,
                    @SerialName("localized_layer") val localizedLayer: String? = null,
                    @SerialName("name") val name: String? = null,
                )

                @Serializable
                data class Metadata(
                    @SerialName("primary_photo")
                    val primaryPhotos: List<PrimaryPhotos> = emptyList(),
                    @SerialName("iso_3166_1") val iso31661: String? = null,
                    @SerialName("iso_3166_2") val iso31662: String? = null,
                    @SerialName("website") val website: String? = null,
                    @SerialName("review_count") val reviewCount: Int? = null,
                    @SerialName("phone") val phone: String? = null,
                    @SerialName("average_rating") val averageRating: Double? = null,
                    @SerialName("open_hours") val openHours: OpenHours? = null,
                    @SerialName("parking") val parking: Parking? = null,
                    @SerialName("raw_plugshare") val rawPlugShare: RawPlugshare? = null,
                    @SerialName("description") val description: String? = null,
                    @SerialName("mapgpt_custom") val mapGptCustom: JsonObject? = null,
                ) {

                    @Serializable
                    data class PrimaryPhotos(
                        @SerialName("width") val width: Int? = null,
                        @SerialName("height") val height: Int? = null,
                        @SerialName("url") val url: String? = null,
                    )

                    @Serializable
                    data class OpenHours(
                        @SerialName("open_type") val openType: String? = null,
                        @SerialName("open_periods") val openPeriods: List<OpenPeriod>? = null,
                    ) {

                        @Serializable
                        data class OpenPeriod(
                            @SerialName("open") val open: Period? = null,
                            @SerialName("close") val close: Period? = null,
                        ) {

                            @Serializable
                            data class Period(
                                @SerialName("day") val day: Int? = null,
                                @SerialName("time") val time: Time? = null,
                            ) {

                                @Serializable
                                data class Time(
                                    @SerialName("hour") val hour: Int? = null,
                                    @SerialName("minute") val minute: Int? = null,
                                )
                            }
                        }
                    }

                    @Serializable
                    data class Parking(
                        @SerialName("capacity") val capacity: Int? = null,
                        @SerialName("reserved_for_disabilities")
                        val reservedForDisabilities: Int? = null,
                        @SerialName("levels") val levels: Int? = null,
                    )

                    @Serializable
                    data class RawPlugshare(
                        @SerialName("id") val id: String? = null,
                        @SerialName("network_id") val networkID: String? = null,
                        @SerialName("outlets") val outlets: List<Outlet> = listOf(),
                        @SerialName("cost_description") val costDescription: String? = null,
                    ) {

                        @Serializable
                        data class Outlet(
                            @SerialName("connector") val connector: Int? = null,
                            @SerialName("id") val id: String? = null,
                            @SerialName("kilowatts") val kilowatts: Int? = null,
                            @SerialName("power") val power: Int? = null,
                        )
                    }
                }
            }
        }
    }

    /**
     * Encode the object to JSON string.
     */
    fun toJsonString(): String {
        return jsonSerialization.encodeToJsonElement(this).toString()
    }

    companion object {

        /**
         * Decode the object from JSON string.
         */
        fun fromJsonString(json: String): SessionFrame {
            return jsonSerialization.decodeFromString(json)
        }
    }
}

@Serializable
@JsonClassDiscriminator(discriminator = "action")
internal abstract class OutgoingSessionFrame {

    @Serializable
    @SerialName("get-session-id")
    class GetSessionId : OutgoingSessionFrame()

    fun toJsonString(): String {
        return jsonSerialization.encodeToString(this)
    }

    companion object {

        fun fromJsonString(json: String): OutgoingSessionFrame {
            return jsonSerialization.decodeFromString(json)
        }
    }
}

package com.mapbox.navigation.mapgpt.core.analytics

import com.mapbox.navigation.mapgpt.core.api.autopilot.AutopilotCarSpeed
import com.mapbox.navigation.mapgpt.core.api.autopilot.AutopilotDrivingStyle
import com.mapbox.navigation.mapgpt.core.api.autopilot.AutopilotLaneDirection
import com.mapbox.navigation.mapgpt.core.api.climate.Occupant
import com.mapbox.navigation.mapgpt.core.api.window.WindowPosition
import com.mapbox.navigation.mapgpt.core.wakeword.WakeWordProvider
import kotlinx.serialization.SerialName

open class MapGptAnalyticsEvent(
    @SerialName("name")
    val name: String,
    @SerialName("params")
    val params: Map<String, MapGptAnalyticsParam<*>>,
) {

    constructor(name: String) : this(name, emptyMap())

    override fun toString(): String {
        return "MapGptAnalyticsEvent(" +
            "name='$name', " +
            "params=$params" +
            ")"
    }

    class Builder(val name: String) {

        private val params = mutableMapOf<String, MapGptAnalyticsParam<*>>()

        fun param(key: String, valueLong: Long?) = apply {
            param(key, valueLong?.let { MapGptAnalyticsParam.LongAnalyticsParam(it) })
        }

        fun param(key: String, valueDouble: Double?) = apply {
            param(key, valueDouble?.let { MapGptAnalyticsParam.DoubleAnalyticsParam(it) })
        }

        fun param(key: String, valueString: String?) = apply {
            param(key, valueString?.let { MapGptAnalyticsParam.StringAnalyticsParam(it) })
        }

        private fun param(key: String, value: MapGptAnalyticsParam<*>?) = apply {
            value?.let { params[key] = value } ?: run { params.remove(key) }
        }

        fun build(): MapGptAnalyticsEvent {
            return MapGptAnalyticsEvent(name, params)
        }
    }
}

class MapGptAssistantActivatedEvent(
    val source: String,
) : MapGptAnalyticsEvent(
    name = "mapgpt_assistant_activated",
    params = mapOf("source" to MapGptAnalyticsParam.StringAnalyticsParam(source)),
)

class MapGptAssistantActivatedWakeWordEvent(
    val provider: WakeWordProvider,
) : MapGptAnalyticsEvent(
    name = "mapgpt_assistant_activated",
    params = mapOf(
        "source" to MapGptAnalyticsParam.StringAnalyticsParam("wakeword"),
        "provider" to MapGptAnalyticsParam.StringAnalyticsParam(provider.key),
    ),
)

class MapGptAssistantStoppedListeningEvent : MapGptAnalyticsEvent(
    name = "mapgpt_assistant_stopped_listening",
)

class MapGptAssistantReplyVoiceEvent : MapGptAnalyticsEvent(
    name = "mapgpt_assistant_reply_voice",
)

class MapGptAssistantFinishReplyVoiceEvent(val assistantResponse: String) : MapGptAnalyticsEvent(
    name = "mapgpt_assistant_finish_reply_voice",
    params = mapOf(
        "responseCharacterCount" to MapGptAnalyticsParam.LongAnalyticsParam(assistantResponse.length.toLong())
    ),
)

class MapGptUserStartedTalkingEvent : MapGptAnalyticsEvent(
    name = "mapgpt_user_started_talking",
)

class MapGptUserStoppedTalkingEvent(val userQuery: String) : MapGptAnalyticsEvent(
    name = "mapgpt_user_stopped_talking",
    params = mapOf(
        "queryCharacterCount" to MapGptAnalyticsParam.LongAnalyticsParam(userQuery.length.toLong()),
        )
)

class MapGptAssistantActivatedWhileTalkingEvent : MapGptAnalyticsEvent(
    name = "mapgpt_assistant_activated_while_talking",
)

class MapGptAssistantInterruptedPressEvent : MapGptAnalyticsEvent(
    name = "mapgpt_assistant_interrupted_press",
)

class MapGptChatAssistantReplyEvent(val assistantResponse: String) : MapGptAnalyticsEvent(
    name = "mapgpt_chat_assistant_reply",
    params = mapOf(
        "responseCharacterCount" to MapGptAnalyticsParam.LongAnalyticsParam(assistantResponse.length.toLong())
    ),
)

class MapGptChatWindowOpenedEvent : MapGptAnalyticsEvent(
    name = "mapgpt_chat_window_opened",
)

class MapGptChatWindowClosedEvent : MapGptAnalyticsEvent(
    name = "mapgpt_chat_window_closed",
)

class MapGptChatUserMessageSentEvent(val userQuery: String) : MapGptAnalyticsEvent(
    name = "mapgpt_chat_user_message_sent",
    params = mapOf(
        "queryCharacterCount" to MapGptAnalyticsParam.LongAnalyticsParam(userQuery.length.toLong()),
    )
)

class MapGptPoiSearchVoiceEvent : MapGptAnalyticsEvent(
    name = "mapgpt_poi_search_voice",
)

class MapGptNavigationActivationEvent : MapGptAnalyticsEvent(
    name = "mapgpt_navigation_activation",
)

class MapGptNavigationCancelledEvent : MapGptAnalyticsEvent(
    name = "mapgpt_navigation_cancelled",
)

class MapGptActionPlayMusicEvent(
    val trackId: String,
) : MapGptAnalyticsEvent(
    name = "mapgpt_action_play_music",
    params = mapOf("track_id" to MapGptAnalyticsParam.StringAnalyticsParam(trackId)),
)

class MapGptActionPauseMusicEvent : MapGptAnalyticsEvent(
    name = "mapgpt_action_pause_music",
)

class MapGptActionResumeMusicEvent : MapGptAnalyticsEvent(
    name = "mapgpt_action_resume_music",
)

class MapGptActionSetTemperatureEvent(
    val unit: String,
    val previousTemperature: Float,
    val currentTemperature: Float,
) : MapGptAnalyticsEvent(
    name = "mapgpt_action_set_temperature",
    params = mapOf(
        "set_temperature" to MapGptAnalyticsParam.StringAnalyticsParam(
            "$previousTemperature $currentTemperature $unit"
        ),
    ),
)

class MapGptActionSetAcEvent(val acTurnOn: Boolean): MapGptAnalyticsEvent(
    name = "mapgpt_action_set_ac",
    params = mapOf(
        "set_ac" to MapGptAnalyticsParam.StringAnalyticsParam("$acTurnOn"),
    ),
)

class MapGptActionSetAutoClimateEvent(val autoClimateTurnOn: Boolean): MapGptAnalyticsEvent(
    name = "mapgpt_action_set_auto",
    params = mapOf(
        "set_auto" to MapGptAnalyticsParam.StringAnalyticsParam("$autoClimateTurnOn"),
    ),
)

class MapGptActionSetDefogEvent(val defogTurnOn: Boolean): MapGptAnalyticsEvent(
    name = "mapgpt_action_set_defog",
    params = mapOf(
        "set_auto" to MapGptAnalyticsParam.StringAnalyticsParam("$defogTurnOn"),
    ),
)

class MapGptActionSetDefrostEvent(val defrostTurnOn: Boolean): MapGptAnalyticsEvent(
    name = "mapgpt_action_set_defrost",
    params = mapOf(
        "set_auto" to MapGptAnalyticsParam.StringAnalyticsParam("$defrostTurnOn"),
    ),
)

class MapGptActionSetWindowEvent(
    val occupant: Occupant,
    val position: WindowPosition,
): MapGptAnalyticsEvent(
    name = "mapgpt_action_set_window",
    params = mapOf(
        "set_window" to MapGptAnalyticsParam.StringAnalyticsParam("${occupant.name} -- ${position.name}"),
    ),
)

class MapGptActionLockWindowEvent(val lockWindow: Boolean): MapGptAnalyticsEvent(
    name = "mapgpt_action_lock_window",
    params = mapOf(
        "lock_window" to MapGptAnalyticsParam.StringAnalyticsParam("$lockWindow"),
    ),
)

class MapGptActionSetSpeedEvent(
    val speed: Int,
    val unit: String,
) : MapGptAnalyticsEvent(
    name = "mapgpt_action_set_speed",
    params = mapOf(
        "set_speed" to MapGptAnalyticsParam.StringAnalyticsParam("$speed $unit"),
    ),
)

class MapGptActionAdjustSpeedEvent(
    val carSpeed: AutopilotCarSpeed,
    val delta: Int?,
    val unit: String,
) : MapGptAnalyticsEvent(
    name = "mapgpt_action_adjust_speed",
    params = mapOf(
        "adjust_speed" to MapGptAnalyticsParam.StringAnalyticsParam("$carSpeed $delta $unit"),
    ),
)

class MapGptActionSetDrivingStyleEvent(
    val drivingStyle: AutopilotDrivingStyle,
) : MapGptAnalyticsEvent(
    name = "mapgpt_action_set_driving_style",
    params = mapOf(
        "set_driving_style" to MapGptAnalyticsParam.StringAnalyticsParam(drivingStyle.name),
    ),
)

class MapGptActionChangeLaneEvent(
    val laneDirection: AutopilotLaneDirection,
    val laneCount: Int,
) : MapGptAnalyticsEvent(
    name = "mapgpt_action_change_lane",
    params = mapOf(
        "change_lane" to MapGptAnalyticsParam.StringAnalyticsParam("$laneDirection $laneCount"),
    ),
)

class MapGptPlayVoiceAnnouncementEvent(
    val voice: String,
) : MapGptAnalyticsEvent(
    name = "mapgpt_play_voice_announcement",
    params = mapOf("voice" to MapGptAnalyticsParam.StringAnalyticsParam(voice)),
)

class MapGptServiceSessionError(
    val error: String,
    val sessionId: String,
    val endpointType: String,
    val languageTag: String,
) : MapGptAnalyticsEvent(
    name = "mapgpt_kmm_error",
    params = mapOf(
        "error" to MapGptAnalyticsParam.StringAnalyticsParam(
            // If parameter's value length is longer than 100, Firebase will drop the event
            error.take(100),
        ),
        "mapGPTSessionId" to MapGptAnalyticsParam.StringAnalyticsParam(sessionId),
        "mapGPTEndpoint" to MapGptAnalyticsParam.StringAnalyticsParam(endpointType),
        "mapGPTLanguage" to MapGptAnalyticsParam.StringAnalyticsParam(languageTag),
    ),
)

class MapGptLatencyOfFirstInteraction(
    val elapsedTimeInMillis: Long,
    val sessionId: String,
    val endpointType: String,
    val languageTag: String,
) :
    MapGptAnalyticsEvent(
        name = "mapgpt_finished_network_processing",
        params = mapOf(
            "finishedConversationDelay" to MapGptAnalyticsParam.LongAnalyticsParam(
                elapsedTimeInMillis,
            ),
            "mapGPTSessionId" to MapGptAnalyticsParam.StringAnalyticsParam(sessionId),
            "mapGPTEndpoint" to MapGptAnalyticsParam.StringAnalyticsParam(endpointType),
            "mapGPTLanguage" to MapGptAnalyticsParam.StringAnalyticsParam(languageTag),
        ),
    )

package com.mapbox.navigation.voicefeedback.internal

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Context required for Feedback Agent service to process the request.
 *
 * @property userContext required app context
 * @property appContext optional user context
 */
@ExperimentalPreviewMapboxNavigationAPI
@Serializable
internal data class FeedbackAgentContextDTO(
    @SerialName("user_context")
    val userContext: FeedbackAgentUserContextDTO,
    @SerialName("app_context")
    val appContext: FeedbackAgentAppContextDTO? = null,
)

/**
 * @property locale IETF language tag (based on ISO 639), for example "en-US".
 * This locale will be used to influence the language the AI replies in.
 * @property temperatureUnits "Fahrenheit" or "Celsius".
 * @property distanceUnits "mi" or "km"
 * @property clientTime The current time in 'yyyy-MM-dd'T'HH:mm:ss' format.
 */
@ExperimentalPreviewMapboxNavigationAPI
@Serializable
internal data class FeedbackAgentAppContextDTO(
    @SerialName("locale")
    val locale: String? = null,
    @SerialName("temp_units")
    val temperatureUnits: String? = null,
    @SerialName("distance_units")
    val distanceUnits: String? = null,
    @SerialName("client_time")
    val clientTime: String? = null,
)

/**
 * @property lat Latitude of the current location.
 * @property lon Longitude of the current location.
 * @property heading Current user heading.
 * @property placeName The name of the place where the user currently is. For example:
 * - Neighborhood, a colloquial sub-city features often referred to in local parlance.
 * - Place, a cities, villages, municipalities, etc.
 * - Locality, a sub-city features present in countries where such an additional administrative layer is used in postal addressing.
 * - District, smaller than top-level administrative features but typically larger than cities.
 * - Region, a top-level sub-national administrative features, such as states in the United States or provinces in Canada or China.
 * - Country, Generally recognized countries or, in some cases like Hong Kong, an area of quasi-national administrative status that has been given a designated country code under ISO 3166-1.
 *
 * The provided name should be the most granular available to be determined (for example, a Neighborhood should be preferred over Place, if available).
 */
@ExperimentalPreviewMapboxNavigationAPI
@Serializable
internal data class FeedbackAgentUserContextDTO(
    val lat: String,
    val lon: String,
    val heading: String? = null,
    @SerialName("place_name")
    val placeName: String,
)

package com.mapbox.navigation.base.route.model

/**
 *
 * @property text A snippet of the full [BannerTextNavigation.text] which can be used for visually
 * altering parts of the full string.
 * @since 1.0
 *
 * @property type String giving you more context about the component which may help in visual markup/display
 * choices. If the type of the components is unknown it should be treated as text.
 *
 * Possible values:
 *
 *  * **text (default)**: indicates the text is part of
 * the instructions and no other type
 *  * **icon**: this is text that can be replaced by an icon, see imageBaseURL
 *  * **delimiter**: this is text that can be dropped and
 * should be dropped if you are rendering icons
 *  * **exit-number**: the exit number for the maneuver
 *  * **exit**: the word for exit in the local language
 * @since 1.0
 *
 * @property abbreviation The abbreviated form of text.
 *
 * If this is present, there will also be an abbr_priority value
 * @since 1.0
 *
 * @property abbreviationPriority An integer indicating the order in which the abbreviation abbr should be used in
 * place of text. The highest priority is 0 and a higher integer value indicates a lower
 * priority. There are no gaps in integer values.
 *
 * Multiple components can have the same abbreviationPriority and when this happens all
 * components with the same abbr_priority should be abbreviated at the same time.
 * Finding no larger values of abbreviationPriority indicates that the string is
 * fully abbreviated.
 * @since 1.0
 *
 * @property imageBaseUrl In some cases when the [LegStepNavigation] is a highway or major roadway,
 * there might be a shield icon that's included to better identify to your user to roadway.
 * Note that this doesn't return the image itself but rather the url which can be used to download the file.
 * @since 1.0
 *
 * @property directions A List of directions indicating which way you can go from a lane
 * (left, right, or straight). If the value is ['left', 'straight'],
 * the driver can go straight or left from that lane.
 * Present if this is a lane component.
 * @since 1.0
 *
 * @property active A boolean telling you if that lane can be used to complete the upcoming maneuver.
 * If multiple lanes are active, then they can all be used to complete the upcoming maneuver.
 * Present if this is a lane component.
 * @since 1.0
 */
data class BannerComponentsNavigation(
    val text: String?,
    val type: String?,
    val abbreviation: String?,
    val abbreviationPriority: Int?,
    val imageBaseUrl: String?,
    val directions: List<String>?,
    val active: Boolean?
)

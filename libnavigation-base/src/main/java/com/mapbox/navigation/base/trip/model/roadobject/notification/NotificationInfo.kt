package com.mapbox.navigation.base.trip.model.roadobject.notification

import androidx.annotation.StringDef
import com.mapbox.api.directions.v5.DirectionsCriteria.NOTIFICATION_TYPE_VIOLATION


/**
 * Class containing detailed information about a notification (see [Notification]).
 *
 * @param type notification type, for possible values see [].
 * @param subtype notification subtype, for possible values see [].
 * @param requestedValue the value passed in route request which was not respected.
 * @param actualValue the value used in route response instead of the value passed in route request.
 * @param unit unit in which requestedValue and actualValue are specified.
 * @param message a string describing the parameter violation.
 */
class NotificationInfo internal constructor(
    @NotificationsType val type: String,
    @NotificationsSubtype val subtype: String?,
    val requestedValue: String?,
    val actualValue: String?,
    val unit: String?,
    val message: String?
) {

    companion object {

        /**
         * Violation notification type. [NotificationInfo.type] will have this value
         * if some request parameters were violated.
         */
        const val NOTIFICATION_TYPE_VIOLATION = "violation"

        /**
         * Max height notification subtype of type [NOTIFICATION_TYPE_VIOLATION].
         * [NotificationInfo.subtype] will have this value if `maxHeight` parameter is violated.
         */
        const val NOTIFICATION_SUBTYPE_MAX_HEIGHT = "maxHeight"

        /**
         * Max width notification subtype of type [NOTIFICATION_TYPE_VIOLATION].
         * [NotificationInfo.subtype] will have this value if `maxWidth` parameter is violated.
         */
        const val NOTIFICATION_SUBTYPE_MAX_WIDTH = "maxWidth"

        /**
         * Max weight notification subtype of type [NOTIFICATION_TYPE_VIOLATION].
         * [NotificationInfo.subtype] will have this value if `maxWeight` parameter is violated.
         */
        const val NOTIFICATION_SUBTYPE_MAX_WEIGHT = "maxWeight"

        /**
         * Unpaved notification subtype of type [NOTIFICATION_TYPE_VIOLATION].
         * [NotificationInfo.subtype] will have this value
         * if `exclude` parameter with value "unpaved" is violated.
         */
        const val NOTIFICATION_SUBTYPE_UNPAVED = "unpaved"

        /**
         * Point exclusion notification subtype of type [NOTIFICATION_TYPE_VIOLATION].
         * [NotificationInfo.subtype] will have this value
         * if `exclude` parameter with point value is violated.
         */
        const val NOTIFICATION_SUBTYPE_POINT_EXCLUSION = "pointExclusion"

        /**
         * Supported notification types. See [NotificationInfo.type].
         */
        @Retention(AnnotationRetention.BINARY)
        @StringDef(
            NOTIFICATION_TYPE_VIOLATION
        )
        annotation class NotificationsType

        /**
         * Supported notification subtypes. See [NotificationInfo.subtype].
         */
        @Retention(AnnotationRetention.BINARY)
        @StringDef(
            NOTIFICATION_SUBTYPE_MAX_HEIGHT,
            NOTIFICATION_SUBTYPE_MAX_WIDTH,
            NOTIFICATION_SUBTYPE_MAX_WEIGHT,
            NOTIFICATION_SUBTYPE_UNPAVED,
            NOTIFICATION_SUBTYPE_POINT_EXCLUSION
        )
        annotation class NotificationsSubtype
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationInfo

        if (type != other.type) return false
        if (subtype != other.subtype) return false
        if (requestedValue != other.requestedValue) return false
        if (actualValue != other.actualValue) return false
        if (unit != other.unit) return false
        if (message != other.message) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (subtype?.hashCode() ?: 0)
        result = 31 * result + (requestedValue?.hashCode() ?: 0)
        result = 31 * result + (actualValue?.hashCode() ?: 0)
        result = 31 * result + (unit?.hashCode() ?: 0)
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NotificationInfo(" +
            "type='$type', " +
            "subtype=$subtype, " +
            "requestedValue=$requestedValue, " +
            "actualValue=$actualValue, " +
            "unit=$unit, " +
            "message=$message" +
            ")"
    }
}

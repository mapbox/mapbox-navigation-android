package com.mapbox.navigation.base.internal.trip.notification

import androidx.annotation.DrawableRes
import com.mapbox.navigation.base.R
import com.mapbox.navigation.base.maneuver.model.BaseTurnIconResources

class NotificationTurnIconResources private constructor(
    @DrawableRes override val turnIconArrive: Int,
    @DrawableRes override val turnIconArriveLeft: Int,
    @DrawableRes override val turnIconArriveRight: Int,
    @DrawableRes override val turnIconArriveStraight: Int,
    @DrawableRes override val turnIconContinue: Int,
    @DrawableRes override val turnIconContinueLeft: Int,
    @DrawableRes override val turnIconContinueRight: Int,
    @DrawableRes override val turnIconContinueStraight: Int,
    @DrawableRes override val turnIconContinueUturn: Int,
    @DrawableRes override val turnIconContinueSlightLeft: Int,
    @DrawableRes override val turnIconContinueSlightRight: Int,
    @DrawableRes override val turnIconDepart: Int,
    @DrawableRes override val turnIconDepartLeft: Int,
    @DrawableRes override val turnIconDepartRight: Int,
    @DrawableRes override val turnIconDepartStraight: Int,
    @DrawableRes override val turnIconEndRoadLeft: Int,
    @DrawableRes override val turnIconEndRoadRight: Int,
    @DrawableRes override val turnIconFork: Int,
    @DrawableRes override val turnIconForkLeft: Int,
    @DrawableRes override val turnIconForkRight: Int,
    @DrawableRes override val turnIconForkStraight: Int,
    @DrawableRes override val turnIconForkSlightLeft: Int,
    @DrawableRes override val turnIconForkSlightRight: Int,
    @DrawableRes override val turnIconInvalid: Int,
    @DrawableRes override val turnIconInvalidLeft: Int,
    @DrawableRes override val turnIconInvalidRight: Int,
    @DrawableRes override val turnIconInvalidStraight: Int,
    @DrawableRes override val turnIconInvalidSlightLeft: Int,
    @DrawableRes override val turnIconInvalidSlightRight: Int,
    @DrawableRes override val turnIconInvalidUturn: Int,
    @DrawableRes override val turnIconMergeLeft: Int,
    @DrawableRes override val turnIconMergeRight: Int,
    @DrawableRes override val turnIconMergeStraight: Int,
    @DrawableRes override val turnIconMergeSlightLeft: Int,
    @DrawableRes override val turnIconMergeSlightRight: Int,
    @DrawableRes override val turnIconNewNameLeft: Int,
    @DrawableRes override val turnIconNewNameRight: Int,
    @DrawableRes override val turnIconNewNameStraight: Int,
    @DrawableRes override val turnIconNewNameSharpLeft: Int,
    @DrawableRes override val turnIconNewNameSharpRight: Int,
    @DrawableRes override val turnIconNewNameSlightLeft: Int,
    @DrawableRes override val turnIconNewNameSlightRight: Int,
    @DrawableRes override val turnIconNotificationLeft: Int,
    @DrawableRes override val turnIconNotificationRight: Int,
    @DrawableRes override val turnIconNotificationStraight: Int,
    @DrawableRes override val turnIconNotificationSharpLeft: Int,
    @DrawableRes override val turnIconNotificationSharpRight: Int,
    @DrawableRes override val turnIconNotificationSlightLeft: Int,
    @DrawableRes override val turnIconNotificationSlightRight: Int,
    @DrawableRes override val turnIconOffRamp: Int,
    @DrawableRes override val turnIconOffRampLeft: Int,
    @DrawableRes override val turnIconOffRampRight: Int,
    @DrawableRes override val turnIconOffRampSlightLeft: Int,
    @DrawableRes override val turnIconOffRampSlightRight: Int,
    @DrawableRes override val turnIconOnRamp: Int,
    @DrawableRes override val turnIconOnRampLeft: Int,
    @DrawableRes override val turnIconOnRampRight: Int,
    @DrawableRes override val turnIconOnRampStraight: Int,
    @DrawableRes override val turnIconOnRampSlightLeft: Int,
    @DrawableRes override val turnIconOnRampSlightRight: Int,
    @DrawableRes override val turnIconOnRampSharpLeft: Int,
    @DrawableRes override val turnIconOnRampSharpRight: Int,
    @DrawableRes override val turnIconRamp: Int,
    @DrawableRes override val turnIconRotary: Int,
    @DrawableRes override val turnIconRotaryLeft: Int,
    @DrawableRes override val turnIconRotaryRight: Int,
    @DrawableRes override val turnIconRotaryStraight: Int,
    @DrawableRes override val turnIconRotarySlightLeft: Int,
    @DrawableRes override val turnIconRotarySlightRight: Int,
    @DrawableRes override val turnIconRotarySharpLeft: Int,
    @DrawableRes override val turnIconRotarySharpRight: Int,
    @DrawableRes override val turnIconRoundabout: Int,
    @DrawableRes override val turnIconRoundaboutLeft: Int,
    @DrawableRes override val turnIconRoundaboutRight: Int,
    @DrawableRes override val turnIconRoundaboutStraight: Int,
    @DrawableRes override val turnIconRoundaboutSlightLeft: Int,
    @DrawableRes override val turnIconRoundaboutSlightRight: Int,
    @DrawableRes override val turnIconRoundaboutSharpLeft: Int,
    @DrawableRes override val turnIconRoundaboutSharpRight: Int,
    @DrawableRes override val turnIconTurnLeft: Int,
    @DrawableRes override val turnIconTurnRight: Int,
    @DrawableRes override val turnIconTurnStraight: Int,
    @DrawableRes override val turnIconTurnSlightLeft: Int,
    @DrawableRes override val turnIconTurnSlightRight: Int,
    @DrawableRes override val turnIconTurnSharpLeft: Int,
    @DrawableRes override val turnIconTurnSharpRight: Int,
    @DrawableRes override val turnIconUturn: Int,
) : BaseTurnIconResources {

    companion object {
        /**
         * Returns default icon set used by Notifications module.
         */
        @JvmStatic
        fun defaultIconSet(): NotificationTurnIconResources = Builder().build()
    }

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .turnIconArrive(turnIconArrive)
            .turnIconArriveLeft(turnIconArriveLeft)
            .turnIconArriveRight(turnIconArriveRight)
            .turnIconArriveStraight(turnIconArriveStraight)
            .turnIconContinue(turnIconContinue)
            .turnIconContinueLeft(turnIconContinueLeft)
            .turnIconContinueRight(turnIconContinueRight)
            .turnIconContinueStraight(turnIconContinueStraight)
            .turnIconContinueUturn(turnIconContinueUturn)
            .turnIconContinueSlightLeft(turnIconContinueSlightLeft)
            .turnIconContinueSlightRight(turnIconContinueSlightRight)
            .turnIconDepart(turnIconDepart)
            .turnIconDepartLeft(turnIconDepartLeft)
            .turnIconDepartRight(turnIconDepartRight)
            .turnIconDepartStraight(turnIconDepartStraight)
            .turnIconEndRoadLeft(turnIconEndRoadLeft)
            .turnIconEndRoadRight(turnIconEndRoadRight)
            .turnIconFork(turnIconFork)
            .turnIconForkLeft(turnIconForkLeft)
            .turnIconForkRight(turnIconForkRight)
            .turnIconForkStraight(turnIconForkStraight)
            .turnIconForkSlightLeft(turnIconForkSlightLeft)
            .turnIconForkSlightRight(turnIconForkSlightRight)
            .turnIconInvalid(turnIconInvalid)
            .turnIconInvalidLeft(turnIconInvalidLeft)
            .turnIconInvalidRight(turnIconInvalidRight)
            .turnIconInvalidStraight(turnIconInvalidStraight)
            .turnIconInvalidUturn(turnIconInvalidUturn)
            .turnIconInvalidSlightLeft(turnIconInvalidSlightLeft)
            .turnIconInvalidSlightRight(turnIconInvalidSlightRight)
            .turnIconMergeLeft(turnIconMergeLeft)
            .turnIconMergeRight(turnIconMergeRight)
            .turnIconMergeStraight(turnIconMergeStraight)
            .turnIconMergeSlightLeft(turnIconMergeSlightLeft)
            .turnIconMergeSlightRight(turnIconMergeSlightRight)
            .turnIconNewNameLeft(turnIconNewNameLeft)
            .turnIconNewNameRight(turnIconNewNameRight)
            .turnIconNewNameStraight(turnIconNewNameStraight)
            .turnIconNewNameSlightLeft(turnIconNewNameSlightLeft)
            .turnIconNewNameSlightRight(turnIconNewNameSlightRight)
            .turnIconNewNameSharpLeft(turnIconNewNameSharpLeft)
            .turnIconNewNameSharpRight(turnIconNewNameSharpRight)
            .turnIconNotificationLeft(turnIconNotificationLeft)
            .turnIconNotificationRight(turnIconNotificationRight)
            .turnIconNotificationStraight(turnIconNotificationStraight)
            .turnIconNotificationSlightLeft(turnIconNotificationSlightLeft)
            .turnIconNotificationSlightRight(turnIconNotificationSlightRight)
            .turnIconNotificationSharpLeft(turnIconNotificationSharpLeft)
            .turnIconNotificationSharpRight(turnIconNotificationSharpRight)
            .turnIconOffRamp(turnIconOffRamp)
            .turnIconOffRampLeft(turnIconOffRampLeft)
            .turnIconOffRampRight(turnIconOffRampRight)
            .turnIconOffRampSlightLeft(turnIconOffRampSlightLeft)
            .turnIconOffRampSlightRight(turnIconOffRampSlightRight)
            .turnIconOnRamp(turnIconOnRamp)
            .turnIconOnRampLeft(turnIconOnRampLeft)
            .turnIconOnRampRight(turnIconOnRampRight)
            .turnIconOnRampStraight(turnIconOnRampStraight)
            .turnIconOnRampSlightLeft(turnIconOnRampSlightLeft)
            .turnIconOnRampSlightRight(turnIconOnRampSlightRight)
            .turnIconOnRampSharpLeft(turnIconOnRampSharpLeft)
            .turnIconOnRampSharpRight(turnIconOnRampSharpRight)
            .turnIconRamp(turnIconRamp)
            .turnIconRotary(turnIconRotary)
            .turnIconRotaryLeft(turnIconRotaryLeft)
            .turnIconRotaryRight(turnIconRotaryRight)
            .turnIconRotaryStraight(turnIconRotaryStraight)
            .turnIconRotarySlightLeft(turnIconRotarySlightLeft)
            .turnIconRotarySlightRight(turnIconRotarySlightRight)
            .turnIconRotarySharpLeft(turnIconRotarySharpLeft)
            .turnIconRotarySharpRight(turnIconRotarySharpRight)
            .turnIconRoundabout(turnIconRoundabout)
            .turnIconRoundaboutLeft(turnIconRoundaboutLeft)
            .turnIconRoundaboutRight(turnIconRoundaboutRight)
            .turnIconRoundaboutStraight(turnIconRoundaboutStraight)
            .turnIconRoundaboutSlightLeft(turnIconRoundaboutSlightLeft)
            .turnIconRoundaboutSlightRight(turnIconRoundaboutSlightRight)
            .turnIconRoundaboutSharpLeft(turnIconRoundaboutSharpLeft)
            .turnIconRoundaboutSharpRight(turnIconRoundaboutSharpRight)
            .turnIconTurnLeft(turnIconTurnLeft)
            .turnIconTurnRight(turnIconTurnRight)
            .turnIconTurnStraight(turnIconTurnStraight)
            .turnIconTurnSlightLeft(turnIconTurnSlightLeft)
            .turnIconTurnSlightRight(turnIconTurnSlightRight)
            .turnIconTurnSharpLeft(turnIconTurnSharpLeft)
            .turnIconTurnSharpRight(turnIconTurnSharpRight)
            .turnIconUturn(turnIconUturn)
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "NotificationTurnIconResources(" +
            "turnIconArrive=$turnIconArrive, " +
            "turnIconArriveLeft=$turnIconArriveLeft, " +
            "turnIconArriveRight=$turnIconArriveRight, " +
            "turnIconArriveStraight=$turnIconArriveStraight, " +
            "turnIconContinue=$turnIconContinue, " +
            "turnIconContinueLeft=$turnIconContinueLeft, " +
            "turnIconContinueRight=$turnIconContinueRight, " +
            "turnIconContinueStraight=$turnIconContinueStraight, " +
            "turnIconContinueUturn=$turnIconContinueUturn, " +
            "turnIconContinueSlightLeft=$turnIconContinueSlightLeft, " +
            "turnIconContinueSlightRight=$turnIconContinueSlightRight, " +
            "turnIconDepart=$turnIconDepart, " +
            "turnIconDepartLeft=$turnIconDepartLeft, " +
            "turnIconDepartRight=$turnIconDepartRight, " +
            "turnIconDepartStraight=$turnIconDepartStraight, " +
            "turnIconEndRoadLeft=$turnIconEndRoadLeft, " +
            "turnIconEndRoadRight=$turnIconEndRoadRight, " +
            "turnIconFork=$turnIconFork, " +
            "turnIconForkLeft=$turnIconForkLeft, " +
            "turnIconForkRight=$turnIconForkRight, " +
            "turnIconForkStraight=$turnIconForkStraight, " +
            "turnIconForkSlightLeft=$turnIconForkSlightLeft, " +
            "turnIconForkSlightRight=$turnIconForkSlightRight, " +
            "turnIconInvalid=$turnIconInvalid, " +
            "turnIconInvalidLeft=$turnIconInvalidLeft, " +
            "turnIconInvalidRight=$turnIconInvalidRight, " +
            "turnIconInvalidStraight=$turnIconInvalidStraight, " +
            "turnIconInvalidSlightLeft=$turnIconInvalidSlightLeft, " +
            "turnIconInvalidSlightRight=$turnIconInvalidSlightRight, " +
            "turnIconMergeLeft=$turnIconMergeLeft, " +
            "turnIconMergeRight=$turnIconMergeRight, " +
            "turnIconMergeStraight=$turnIconMergeStraight, " +
            "turnIconMergeSlightLeft=$turnIconMergeSlightLeft, " +
            "turnIconMergeSlightRight=$turnIconMergeSlightRight, " +
            "turnIconNewNameLeft=$turnIconNewNameLeft, " +
            "turnIconNewNameRight=$turnIconNewNameRight, " +
            "turnIconNewNameStraight=$turnIconNewNameStraight, " +
            "turnIconNewNameSlightLeft=$turnIconNewNameSlightLeft, " +
            "turnIconNewNameSlightRight=$turnIconNewNameSlightRight, " +
            "turnIconNewNameSharpLeft=$turnIconNewNameSharpLeft, " +
            "turnIconNewNameSharpRight=$turnIconNewNameSharpRight, " +
            "turnIconNotificationLeft=$turnIconNotificationLeft, " +
            "turnIconNotificationRight=$turnIconNotificationRight, " +
            "turnIconNotificationStraight=$turnIconNotificationStraight, " +
            "turnIconNotificationSlightLeft=$turnIconNotificationSlightLeft, " +
            "turnIconNotificationSlightRight=$turnIconNotificationSlightRight, " +
            "turnIconNotificationSharpLeft=$turnIconNotificationSharpLeft, " +
            "turnIconNotificationSharpRight=$turnIconNotificationSharpRight, " +
            "turnIconOffRamp=$turnIconOffRamp, " +
            "turnIconOffRampLeft=$turnIconOffRampLeft, " +
            "turnIconOffRampRight=$turnIconOffRampRight, " +
            "turnIconOffRampSlightLeft=$turnIconOffRampSlightLeft, " +
            "turnIconOffRampSlightRight=$turnIconOffRampSlightRight, " +
            "turnIconOnRamp=$turnIconOnRamp, " +
            "turnIconOnRampLeft=$turnIconOnRampLeft, " +
            "turnIconOnRampRight=$turnIconOnRampRight, " +
            "turnIconOnRampStraight=$turnIconOnRampStraight, " +
            "turnIconOnRampSlightLeft=$turnIconOnRampSlightLeft, " +
            "turnIconOnRampSlightRight=$turnIconOnRampSlightRight, " +
            "turnIconOnRampSharpLeft=$turnIconOnRampSharpLeft, " +
            "turnIconOnRampSharpRight=$turnIconOnRampSharpRight, " +
            "turnIconRamp=$turnIconRamp, " +
            "turnIconRotary=$turnIconRotary, " +
            "turnIconRotaryLeft=$turnIconRotaryLeft, " +
            "turnIconRotaryRight=$turnIconRotaryRight, " +
            "turnIconRotaryStraight=$turnIconRotaryStraight, " +
            "turnIconRotarySlightLeft=$turnIconRotarySlightLeft, " +
            "turnIconRotarySlightRight=$turnIconRotarySlightRight, " +
            "turnIconRotarySharpLeft=$turnIconRotarySharpLeft, " +
            "turnIconRotarySharpRight=$turnIconRotarySharpRight, " +
            "turnIconRoundabout=$turnIconRoundabout, " +
            "turnIconRoundaboutLeft=$turnIconRoundaboutLeft, " +
            "turnIconRoundaboutRight=$turnIconRoundaboutRight, " +
            "turnIconRoundaboutStraight=$turnIconRoundaboutStraight, " +
            "turnIconRoundaboutSlightLeft=$turnIconRoundaboutSlightLeft, " +
            "turnIconRoundaboutSlightRight=$turnIconRoundaboutSlightRight, " +
            "turnIconRoundaboutSharpLeft=$turnIconRoundaboutSharpLeft, " +
            "turnIconRoundaboutSharpRight=$turnIconRoundaboutSharpRight, " +
            "turnIconTurnLeft=$turnIconTurnLeft, " +
            "turnIconTurnRight=$turnIconTurnRight, " +
            "turnIconTurnStraight=$turnIconTurnStraight, " +
            "turnIconTurnSlightLeft=$turnIconTurnSlightLeft, " +
            "turnIconTurnSlightRight=$turnIconTurnSlightRight, " +
            "turnIconTurnSharpLeft=$turnIconTurnSharpLeft, " +
            "turnIconTurnSharpRight=$turnIconTurnSharpRight, " +
            "turnIconUturn=$turnIconUturn" +
            ")"
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NotificationTurnIconResources

        if (turnIconArrive != other.turnIconArrive) return false
        if (turnIconArriveLeft != other.turnIconArriveLeft) return false
        if (turnIconArriveRight != other.turnIconArriveRight) return false
        if (turnIconArriveStraight != other.turnIconArriveStraight) return false
        if (turnIconContinue != other.turnIconContinue) return false
        if (turnIconContinueLeft != other.turnIconContinueLeft) return false
        if (turnIconContinueRight != other.turnIconContinueRight) return false
        if (turnIconContinueStraight != other.turnIconContinueStraight) return false
        if (turnIconContinueUturn != other.turnIconContinueUturn) return false
        if (turnIconContinueSlightLeft != other.turnIconContinueSlightLeft) return false
        if (turnIconContinueSlightRight != other.turnIconContinueSlightRight) return false
        if (turnIconDepart != other.turnIconDepart) return false
        if (turnIconDepartLeft != other.turnIconDepartLeft) return false
        if (turnIconDepartRight != other.turnIconDepartRight) return false
        if (turnIconDepartStraight != other.turnIconDepartStraight) return false
        if (turnIconEndRoadLeft != other.turnIconEndRoadLeft) return false
        if (turnIconEndRoadRight != other.turnIconEndRoadRight) return false
        if (turnIconFork != other.turnIconFork) return false
        if (turnIconForkLeft != other.turnIconForkLeft) return false
        if (turnIconForkRight != other.turnIconForkRight) return false
        if (turnIconForkStraight != other.turnIconForkStraight) return false
        if (turnIconForkSlightLeft != other.turnIconForkSlightLeft) return false
        if (turnIconForkSlightRight != other.turnIconForkSlightRight) return false
        if (turnIconInvalid != other.turnIconInvalid) return false
        if (turnIconInvalidLeft != other.turnIconInvalidLeft) return false
        if (turnIconInvalidRight != other.turnIconInvalidRight) return false
        if (turnIconInvalidStraight != other.turnIconInvalidStraight) return false
        if (turnIconInvalidSlightLeft != other.turnIconInvalidSlightLeft) return false
        if (turnIconInvalidSlightRight != other.turnIconInvalidSlightRight) return false
        if (turnIconInvalidUturn != other.turnIconInvalidUturn) return false
        if (turnIconMergeLeft != other.turnIconMergeLeft) return false
        if (turnIconMergeRight != other.turnIconMergeRight) return false
        if (turnIconMergeStraight != other.turnIconMergeStraight) return false
        if (turnIconMergeSlightLeft != other.turnIconMergeSlightLeft) return false
        if (turnIconMergeSlightRight != other.turnIconMergeSlightRight) return false
        if (turnIconNewNameLeft != other.turnIconNewNameLeft) return false
        if (turnIconNewNameRight != other.turnIconNewNameRight) return false
        if (turnIconNewNameStraight != other.turnIconNewNameStraight) return false
        if (turnIconNewNameSharpLeft != other.turnIconNewNameSharpLeft) return false
        if (turnIconNewNameSharpRight != other.turnIconNewNameSharpRight) return false
        if (turnIconNewNameSlightLeft != other.turnIconNewNameSlightLeft) return false
        if (turnIconNewNameSlightRight != other.turnIconNewNameSlightRight) return false
        if (turnIconNotificationLeft != other.turnIconNotificationLeft) return false
        if (turnIconNotificationRight != other.turnIconNotificationRight) return false
        if (turnIconNotificationStraight != other.turnIconNotificationStraight) return false
        if (turnIconNotificationSharpLeft != other.turnIconNotificationSharpLeft) return false
        if (turnIconNotificationSharpRight != other.turnIconNotificationSharpRight) return false
        if (turnIconNotificationSlightLeft != other.turnIconNotificationSlightLeft) return false
        if (turnIconNotificationSlightRight != other.turnIconNotificationSlightRight) return false
        if (turnIconOffRamp != other.turnIconOffRamp) return false
        if (turnIconOffRampLeft != other.turnIconOffRampLeft) return false
        if (turnIconOffRampRight != other.turnIconOffRampRight) return false
        if (turnIconOffRampSlightLeft != other.turnIconOffRampSlightLeft) return false
        if (turnIconOffRampSlightRight != other.turnIconOffRampSlightRight) return false
        if (turnIconOnRamp != other.turnIconOnRamp) return false
        if (turnIconOnRampLeft != other.turnIconOnRampLeft) return false
        if (turnIconOnRampRight != other.turnIconOnRampRight) return false
        if (turnIconOnRampStraight != other.turnIconOnRampStraight) return false
        if (turnIconOnRampSlightLeft != other.turnIconOnRampSlightLeft) return false
        if (turnIconOnRampSlightRight != other.turnIconOnRampSlightRight) return false
        if (turnIconOnRampSharpLeft != other.turnIconOnRampSharpLeft) return false
        if (turnIconOnRampSharpRight != other.turnIconOnRampSharpRight) return false
        if (turnIconRamp != other.turnIconRamp) return false
        if (turnIconRotary != other.turnIconRotary) return false
        if (turnIconRotaryLeft != other.turnIconRotaryLeft) return false
        if (turnIconRotaryRight != other.turnIconRotaryRight) return false
        if (turnIconRotaryStraight != other.turnIconRotaryStraight) return false
        if (turnIconRotarySlightLeft != other.turnIconRotarySlightLeft) return false
        if (turnIconRotarySlightRight != other.turnIconRotarySlightLeft) return false
        if (turnIconRotarySharpLeft != other.turnIconRotarySharpLeft) return false
        if (turnIconRotarySharpRight != other.turnIconRotarySharpRight) return false
        if (turnIconRoundabout != other.turnIconRoundabout) return false
        if (turnIconRoundaboutLeft != other.turnIconRoundaboutLeft) return false
        if (turnIconRoundaboutRight != other.turnIconRoundaboutRight) return false
        if (turnIconRoundaboutStraight != other.turnIconRoundaboutStraight) return false
        if (turnIconRoundaboutSlightLeft != other.turnIconRoundaboutSlightLeft) return false
        if (turnIconRoundaboutSlightRight != other.turnIconRoundaboutSlightRight) return false
        if (turnIconRoundaboutSharpLeft != other.turnIconRoundaboutSharpLeft) return false
        if (turnIconRoundaboutSharpRight != other.turnIconRoundaboutSharpRight) return false
        if (turnIconTurnLeft != other.turnIconTurnLeft) return false
        if (turnIconTurnRight != other.turnIconTurnRight) return false
        if (turnIconTurnStraight != other.turnIconTurnStraight) return false
        if (turnIconTurnSlightLeft != other.turnIconTurnSlightLeft) return false
        if (turnIconTurnSlightRight != other.turnIconTurnSlightRight) return false
        if (turnIconTurnSharpLeft != other.turnIconTurnSharpLeft) return false
        if (turnIconTurnSharpRight != other.turnIconTurnSharpRight) return false
        if (turnIconUturn != other.turnIconUturn) {
            return false
        }

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = turnIconArrive
        result = 31 * result + turnIconArriveLeft
        result = 31 * result + turnIconArriveRight
        result = 31 * result + turnIconArriveStraight
        result = 31 * result + turnIconContinue
        result = 31 * result + turnIconContinueLeft
        result = 31 * result + turnIconContinueRight
        result = 31 * result + turnIconContinueUturn
        result = 31 * result + turnIconContinueStraight
        result = 31 * result + turnIconContinueSlightLeft
        result = 31 * result + turnIconContinueSlightRight
        result = 31 * result + turnIconDepart
        result = 31 * result + turnIconDepartLeft
        result = 31 * result + turnIconDepartRight
        result = 31 * result + turnIconDepartStraight
        result = 31 * result + turnIconEndRoadLeft
        result = 31 * result + turnIconEndRoadRight
        result = 31 * result + turnIconFork
        result = 31 * result + turnIconForkLeft
        result = 31 * result + turnIconForkRight
        result = 31 * result + turnIconForkStraight
        result = 31 * result + turnIconForkSlightLeft
        result = 31 * result + turnIconForkSlightRight
        result = 31 * result + turnIconInvalid
        result = 31 * result + turnIconInvalidLeft
        result = 31 * result + turnIconInvalidRight
        result = 31 * result + turnIconInvalidStraight
        result = 31 * result + turnIconInvalidSlightLeft
        result = 31 * result + turnIconInvalidSlightRight
        result = 31 * result + turnIconInvalidUturn
        result = 31 * result + turnIconMergeLeft
        result = 31 * result + turnIconMergeRight
        result = 31 * result + turnIconMergeStraight
        result = 31 * result + turnIconMergeSlightLeft
        result = 31 * result + turnIconMergeSlightRight
        result = 31 * result + turnIconNewNameLeft
        result = 31 * result + turnIconNewNameRight
        result = 31 * result + turnIconNewNameStraight
        result = 31 * result + turnIconNewNameSharpLeft
        result = 31 * result + turnIconNewNameSharpRight
        result = 31 * result + turnIconNewNameSlightLeft
        result = 31 * result + turnIconNewNameSlightRight
        result = 31 * result + turnIconNotificationLeft
        result = 31 * result + turnIconNotificationRight
        result = 31 * result + turnIconNotificationStraight
        result = 31 * result + turnIconNotificationSharpLeft
        result = 31 * result + turnIconNotificationSharpRight
        result = 31 * result + turnIconNotificationSlightLeft
        result = 31 * result + turnIconNotificationSlightRight
        result = 31 * result + turnIconOffRamp
        result = 31 * result + turnIconOffRampLeft
        result = 31 * result + turnIconOffRampRight
        result = 31 * result + turnIconOffRampSlightLeft
        result = 31 * result + turnIconOffRampSlightRight
        result = 31 * result + turnIconOnRamp
        result = 31 * result + turnIconOnRampLeft
        result = 31 * result + turnIconOnRampRight
        result = 31 * result + turnIconOnRampStraight
        result = 31 * result + turnIconOnRampSlightLeft
        result = 31 * result + turnIconOnRampSlightRight
        result = 31 * result + turnIconOnRampSharpLeft
        result = 31 * result + turnIconOnRampSharpRight
        result = 31 * result + turnIconRamp
        result = 31 * result + turnIconRotary
        result = 31 * result + turnIconRotaryLeft
        result = 31 * result + turnIconRotaryRight
        result = 31 * result + turnIconRotaryStraight
        result = 31 * result + turnIconRotarySlightLeft
        result = 31 * result + turnIconRotarySlightRight
        result = 31 * result + turnIconRotarySharpLeft
        result = 31 * result + turnIconRotarySharpRight
        result = 31 * result + turnIconRoundabout
        result = 31 * result + turnIconRoundaboutLeft
        result = 31 * result + turnIconRoundaboutRight
        result = 31 * result + turnIconRoundaboutStraight
        result = 31 * result + turnIconRoundaboutSlightLeft
        result = 31 * result + turnIconRoundaboutSlightRight
        result = 31 * result + turnIconRoundaboutSharpLeft
        result = 31 * result + turnIconRoundaboutSharpRight
        result = 31 * result + turnIconTurnLeft
        result = 31 * result + turnIconTurnRight
        result = 31 * result + turnIconTurnStraight
        result = 31 * result + turnIconTurnSlightLeft
        result = 31 * result + turnIconTurnSlightRight
        result = 31 * result + turnIconTurnSharpLeft
        result = 31 * result + turnIconTurnSharpRight
        result = 31 * result + turnIconUturn
        return result
    }

    /**
     * Build a new [NotificationTurnIconResources]
     * @property turnIconArrive Int
     * @property turnIconArriveLeft Int
     * @property turnIconArriveRight Int
     * @property turnIconArriveStraight Int
     * @property turnIconContinue Int
     * @property turnIconContinueLeft Int
     * @property turnIconContinueRight Int
     * @property turnIconContinueStraight Int
     * @property turnIconContinueUturn Int
     * @property turnIconContinueSlightLeft Int
     * @property turnIconContinueSlightRight Int
     * @property turnIconDepart Int
     * @property turnIconDepartLeft Int
     * @property turnIconDepartRight Int
     * @property turnIconDepartStraight Int
     * @property turnIconEndRoadLeft Int
     * @property turnIconEndRoadRight Int
     * @property turnIconFork Int
     * @property turnIconForkLeft Int
     * @property turnIconForkRight Int
     * @property turnIconForkStraight Int
     * @property turnIconForkSlightLeft Int
     * @property turnIconForkSlightRight Int
     * @property turnIconInvalid Int
     * @property turnIconInvalidLeft Int
     * @property turnIconInvalidRight Int
     * @property turnIconInvalidStraight Int
     * @property turnIconInvalidSlightLeft Int
     * @property turnIconInvalidSlightRight Int
     * @property turnIconInvalidUturn Int
     * @property turnIconMergeLeft Int
     * @property turnIconMergeRight Int
     * @property turnIconMergeStraight Int
     * @property turnIconMergeSlightLeft Int
     * @property turnIconMergeSlightRight Int
     * @property turnIconNewNameLeft Int
     * @property turnIconNewNameRight Int
     * @property turnIconNewNameStraight Int
     * @property turnIconNewNameSharpLeft Int
     * @property turnIconNewNameSharpRight Int
     * @property turnIconNewNameSlightLeft Int
     * @property turnIconNewNameSlightRight Int
     * @property turnIconNotificationLeft Int
     * @property turnIconNotificationRight Int
     * @property turnIconNotificationStraight Int
     * @property turnIconNotificationSharpLeft Int
     * @property turnIconNotificationSharpRight Int
     * @property turnIconNotificationSlightLeft Int
     * @property turnIconNotificationSlightRight Int
     * @property turnIconOffRamp Int
     * @property turnIconOffRampLeft Int
     * @property turnIconOffRampRight Int
     * @property turnIconOffRampSlightLeft Int
     * @property turnIconOffRampSlightRight Int
     * @property turnIconOnRamp Int
     * @property turnIconOnRampLeft Int
     * @property turnIconOnRampRight Int
     * @property turnIconOnRampStraight Int
     * @property turnIconOnRampSlightLeft Int
     * @property turnIconOnRampSlightRight Int
     * @property turnIconOnRampSharpLeft Int
     * @property turnIconOnRampSharpRight Int
     * @property turnIconRamp Int
     * @property turnIconRotary Int
     * @property turnIconRotaryLeft Int
     * @property turnIconRotaryRight Int
     * @property turnIconRotaryStraight Int
     * @property turnIconRotarySlightLeft Int
     * @property turnIconRotarySlightRight Int
     * @property turnIconRotarySharpLeft Int
     * @property turnIconRotarySharpRight Int
     * @property turnIconRoundabout Int
     * @property turnIconRoundaboutLeft Int
     * @property turnIconRoundaboutRight Int
     * @property turnIconRoundaboutStraight Int
     * @property turnIconRoundaboutSlightLeft Int
     * @property turnIconRoundaboutSlightRight Int
     * @property turnIconRoundaboutSharpLeft Int
     * @property turnIconRoundaboutSharpRight Int
     * @property turnIconTurnLeft Int
     * @property turnIconTurnRight Int
     * @property turnIconTurnStraight Int
     * @property turnIconTurnSlightLeft Int
     * @property turnIconTurnSlightRight Int
     * @property turnIconTurnSharpLeft Int
     * @property turnIconTurnSharpRight Int
     * @property turnIconUturn Int
     */
    class Builder {
        private var turnIconArrive: Int = R.drawable.mapbox_ic_notification_arrive
        private var turnIconArriveLeft: Int = R.drawable.mapbox_ic_notification_arrive_left
        private var turnIconArriveRight: Int = R.drawable.mapbox_ic_notification_arrive_right
        private var turnIconArriveStraight: Int = R.drawable.mapbox_ic_notification_arrive_straight
        private var turnIconContinue: Int = R.drawable.mapbox_ic_notification_turn_straight
        private var turnIconContinueLeft: Int = R.drawable.mapbox_ic_notification_turn_left
        private var turnIconContinueRight: Int = R.drawable.mapbox_ic_notification_turn_right
        private var turnIconContinueStraight: Int = R.drawable.mapbox_ic_notification_turn_straight
        private var turnIconContinueUturn: Int = R.drawable.mapbox_ic_notification_uturn
        private var turnIconContinueSlightLeft: Int =
            R.drawable.mapbox_ic_notification_turn_slight_left
        private var turnIconContinueSlightRight: Int =
            R.drawable.mapbox_ic_notification_turn_slight_right
        private var turnIconDepart: Int = R.drawable.mapbox_ic_notification_depart
        private var turnIconDepartLeft: Int = R.drawable.mapbox_ic_notification_depart_left
        private var turnIconDepartRight: Int = R.drawable.mapbox_ic_notification_depart_right
        private var turnIconDepartStraight: Int = R.drawable.mapbox_ic_notification_depart_straight
        private var turnIconEndRoadLeft: Int = R.drawable.mapbox_ic_notification_end_of_road_left
        private var turnIconEndRoadRight: Int = R.drawable.mapbox_ic_notification_end_of_road_right
        private var turnIconFork: Int = R.drawable.mapbox_ic_notification_fork
        private var turnIconForkLeft: Int = R.drawable.mapbox_ic_notification_fork_left
        private var turnIconForkRight: Int = R.drawable.mapbox_ic_notification_fork_right
        private var turnIconForkStraight: Int = R.drawable.mapbox_ic_notification_fork_straight
        private var turnIconForkSlightLeft: Int = R.drawable.mapbox_ic_notification_fork_slight_left
        private var turnIconForkSlightRight: Int =
            R.drawable.mapbox_ic_notification_fork_slight_right
        private var turnIconInvalid: Int = R.drawable.mapbox_ic_notification_turn_straight
        private var turnIconInvalidLeft: Int = R.drawable.mapbox_ic_notification_turn_left
        private var turnIconInvalidRight: Int = R.drawable.mapbox_ic_notification_turn_right
        private var turnIconInvalidStraight: Int = R.drawable.mapbox_ic_notification_turn_straight
        private var turnIconInvalidSlightLeft: Int =
            R.drawable.mapbox_ic_notification_turn_slight_left
        private var turnIconInvalidSlightRight: Int =
            R.drawable.mapbox_ic_notification_turn_slight_right
        private var turnIconInvalidUturn: Int = R.drawable.mapbox_ic_notification_uturn
        private var turnIconMergeLeft: Int = R.drawable.mapbox_ic_notification_merge_left
        private var turnIconMergeRight: Int = R.drawable.mapbox_ic_notification_merge_right
        private var turnIconMergeStraight: Int = R.drawable.mapbox_ic_notification_turn_straight
        private var turnIconMergeSlightLeft: Int =
            R.drawable.mapbox_ic_notification_merge_slight_left
        private var turnIconMergeSlightRight: Int =
            R.drawable.mapbox_ic_notification_merge_slight_right
        private var turnIconNewNameLeft: Int = R.drawable.mapbox_ic_notification_turn_left
        private var turnIconNewNameRight: Int = R.drawable.mapbox_ic_notification_turn_right
        private var turnIconNewNameStraight: Int = R.drawable.mapbox_ic_notification_turn_straight
        private var turnIconNewNameSharpLeft: Int =
            R.drawable.mapbox_ic_notification_turn_sharp_left
        private var turnIconNewNameSharpRight: Int =
            R.drawable.mapbox_ic_notification_turn_sharp_right
        private var turnIconNewNameSlightLeft: Int =
            R.drawable.mapbox_ic_notification_turn_slight_left
        private var turnIconNewNameSlightRight: Int =
            R.drawable.mapbox_ic_notification_turn_slight_right
        private var turnIconNotificationLeft: Int = R.drawable.mapbox_ic_notification_turn_left
        private var turnIconNotificationRight: Int = R.drawable.mapbox_ic_notification_turn_right
        private var turnIconNotificationStraight: Int =
            R.drawable.mapbox_ic_notification_turn_straight
        private var turnIconNotificationSharpLeft: Int =
            R.drawable.mapbox_ic_notification_turn_sharp_left
        private var turnIconNotificationSharpRight: Int =
            R.drawable.mapbox_ic_notification_turn_sharp_right
        private var turnIconNotificationSlightLeft: Int =
            R.drawable.mapbox_ic_notification_turn_slight_left
        private var turnIconNotificationSlightRight: Int =
            R.drawable.mapbox_ic_notification_turn_slight_right
        private var turnIconOffRamp: Int = R.drawable.mapbox_ic_notification_off_ramp
        private var turnIconOffRampLeft: Int = R.drawable.mapbox_ic_notification_off_ramp_left
        private var turnIconOffRampRight: Int = R.drawable.mapbox_ic_notification_off_ramp_right
        private var turnIconOffRampSlightLeft: Int =
            R.drawable.mapbox_ic_notification_off_ramp_slight_left
        private var turnIconOffRampSlightRight: Int =
            R.drawable.mapbox_ic_notification_off_ramp_slight_right
        private var turnIconOnRamp: Int = R.drawable.mapbox_ic_notification_on_ramp
        private var turnIconOnRampLeft: Int = R.drawable.mapbox_ic_notification_turn_left
        private var turnIconOnRampRight: Int = R.drawable.mapbox_ic_notification_turn_right
        private var turnIconOnRampStraight: Int = R.drawable.mapbox_ic_notification_turn_straight
        private var turnIconOnRampSlightLeft: Int =
            R.drawable.mapbox_ic_notification_turn_slight_left
        private var turnIconOnRampSlightRight: Int =
            R.drawable.mapbox_ic_notification_turn_slight_right
        private var turnIconOnRampSharpLeft: Int =
            R.drawable.mapbox_ic_notification_turn_sharp_left
        private var turnIconOnRampSharpRight: Int =
            R.drawable.mapbox_ic_notification_turn_sharp_right
        private var turnIconRamp: Int = R.drawable.mapbox_ic_notification_ramp
        private var turnIconRotary: Int = R.drawable.mapbox_ic_notification_rotary
        private var turnIconRotaryLeft: Int = R.drawable.mapbox_ic_notification_rotary_left
        private var turnIconRotaryRight: Int = R.drawable.mapbox_ic_notification_rotary_right
        private var turnIconRotaryStraight: Int = R.drawable.mapbox_ic_notification_rotary_straight
        private var turnIconRotarySlightLeft: Int =
            R.drawable.mapbox_ic_notification_rotary_slight_left
        private var turnIconRotarySlightRight: Int =
            R.drawable.mapbox_ic_notification_rotary_slight_right
        private var turnIconRotarySharpLeft: Int =
            R.drawable.mapbox_ic_notification_rotary_sharp_left
        private var turnIconRotarySharpRight: Int =
            R.drawable.mapbox_ic_notification_rotary_sharp_right
        private var turnIconRoundabout: Int = R.drawable.mapbox_ic_notification_roundabout
        private var turnIconRoundaboutLeft: Int = R.drawable.mapbox_ic_notification_roundabout_left
        private var turnIconRoundaboutRight: Int =
            R.drawable.mapbox_ic_notification_roundabout_right
        private var turnIconRoundaboutStraight: Int =
            R.drawable.mapbox_ic_notification_roundabout_straight
        private var turnIconRoundaboutSlightLeft: Int =
            R.drawable.mapbox_ic_notification_roundabout_slight_left
        private var turnIconRoundaboutSlightRight: Int =
            R.drawable.mapbox_ic_notification_roundabout_slight_right
        private var turnIconRoundaboutSharpLeft: Int =
            R.drawable.mapbox_ic_notification_roundabout_sharp_left
        private var turnIconRoundaboutSharpRight: Int =
            R.drawable.mapbox_ic_notification_roundabout_sharp_right
        private var turnIconTurnLeft: Int = R.drawable.mapbox_ic_notification_turn_left
        private var turnIconTurnRight: Int = R.drawable.mapbox_ic_notification_turn_right
        private var turnIconTurnStraight: Int = R.drawable.mapbox_ic_notification_turn_straight
        private var turnIconTurnSlightLeft: Int = R.drawable.mapbox_ic_notification_turn_slight_left
        private var turnIconTurnSlightRight: Int =
            R.drawable.mapbox_ic_notification_turn_slight_right
        private var turnIconTurnSharpLeft: Int = R.drawable.mapbox_ic_notification_turn_sharp_left
        private var turnIconTurnSharpRight: Int = R.drawable.mapbox_ic_notification_turn_sharp_right
        private var turnIconUturn: Int = R.drawable.mapbox_ic_notification_uturn

        /**
         * apply arrive icon to the builder.
         * @param turnIconArrive Int
         * @return Builder
         */
        fun turnIconArrive(@DrawableRes turnIconArrive: Int): Builder =
            apply { this.turnIconArrive = turnIconArrive }

        /**
         * apply arrive left icon to the builder.
         * @param turnIconArriveLeft Int
         * @return Builder
         */
        fun turnIconArriveLeft(@DrawableRes turnIconArriveLeft: Int): Builder =
            apply { this.turnIconArriveLeft = turnIconArriveLeft }

        /**
         * apply arrive right icon to the builder.
         * @param turnIconArriveRight Int
         * @return Builder
         */
        fun turnIconArriveRight(@DrawableRes turnIconArriveRight: Int): Builder =
            apply { this.turnIconArriveRight = turnIconArriveRight }

        /**
         * apply arrive straight icon to the builder.
         * @param turnIconArriveStraight Int
         * @return Builder
         */
        fun turnIconArriveStraight(@DrawableRes turnIconArriveStraight: Int): Builder =
            apply { this.turnIconArriveStraight = turnIconArriveStraight }

        /**
         * apply continue icon to the builder.
         * @param turnIconContinue Int
         * @return Builder
         */
        fun turnIconContinue(@DrawableRes turnIconContinue: Int): Builder =
            apply { this.turnIconContinue = turnIconContinue }

        /**
         * apply continue left icon to the builder.
         * @param turnIconContinueLeft Int
         * @return Builder
         */
        fun turnIconContinueLeft(@DrawableRes turnIconContinueLeft: Int): Builder =
            apply { this.turnIconContinueLeft = turnIconContinueLeft }

        /**
         * apply continue right icon to the builder.
         * @param turnIconContinueRight Int
         * @return Builder
         */
        fun turnIconContinueRight(@DrawableRes turnIconContinueRight: Int): Builder =
            apply { this.turnIconContinueRight = turnIconContinueRight }

        /**
         * apply continue straight icon to the builder.
         * @param turnIconContinueStraight Int
         * @return Builder
         */
        fun turnIconContinueStraight(@DrawableRes turnIconContinueStraight: Int): Builder =
            apply { this.turnIconContinueStraight = turnIconContinueStraight }

        /**
         * apply continue uturn icon to the builder.
         * @param turnIconContinueUturn Int
         * @return Builder
         */
        fun turnIconContinueUturn(@DrawableRes turnIconContinueUturn: Int): Builder =
            apply { this.turnIconContinueUturn = turnIconContinueUturn }

        /**
         * apply continue slight left icon to the builder.
         * @param turnIconContinueSlightLeft Int
         * @return Builder
         */
        fun turnIconContinueSlightLeft(@DrawableRes turnIconContinueSlightLeft: Int): Builder =
            apply { this.turnIconContinueSlightLeft = turnIconContinueSlightLeft }

        /**
         * apply continue slight right icon to the builder.
         * @param turnIconContinueSlightRight Int
         * @return Builder
         */
        fun turnIconContinueSlightRight(@DrawableRes turnIconContinueSlightRight: Int): Builder =
            apply { this.turnIconContinueSlightRight = turnIconContinueSlightRight }

        /**
         * apply depart icon to the builder.
         * @param turnIconDepart Int
         * @return Builder
         */
        fun turnIconDepart(@DrawableRes turnIconDepart: Int): Builder =
            apply { this.turnIconDepart = turnIconDepart }

        /**
         * apply depart left icon to the builder.
         * @param turnIconDepartLeft Int
         * @return Builder
         */
        fun turnIconDepartLeft(@DrawableRes turnIconDepartLeft: Int): Builder =
            apply { this.turnIconDepartLeft = turnIconDepartLeft }

        /**
         * apply depart right icon to the builder.
         * @param turnIconDepartRight Int
         * @return Builder
         */
        fun turnIconDepartRight(@DrawableRes turnIconDepartRight: Int): Builder =
            apply { this.turnIconDepartRight = turnIconDepartRight }

        /**
         * apply depart straight icon to the builder.
         * @param turnIconDepartStraight Int
         * @return Builder
         */
        fun turnIconDepartStraight(@DrawableRes turnIconDepartStraight: Int): Builder =
            apply { this.turnIconDepartStraight = turnIconDepartStraight }

        /**
         * apply end road left icon to the builder.
         * @param turnIconEndRoadLeft Int
         * @return Builder
         */
        fun turnIconEndRoadLeft(@DrawableRes turnIconEndRoadLeft: Int): Builder =
            apply { this.turnIconEndRoadLeft = turnIconEndRoadLeft }

        /**
         * apply end road right icon to the builder.
         * @param turnIconEndRoadRight Int
         * @return Builder
         */
        fun turnIconEndRoadRight(@DrawableRes turnIconEndRoadRight: Int): Builder =
            apply { this.turnIconEndRoadRight = turnIconEndRoadRight }

        /**
         * apply fork icon to the builder.
         * @param turnIconFork Int
         * @return Builder
         */
        fun turnIconFork(@DrawableRes turnIconFork: Int): Builder =
            apply { this.turnIconFork = turnIconFork }

        /**
         * apply fork left icon to the builder.
         * @param turnIconForkLeft Int
         * @return Builder
         */
        fun turnIconForkLeft(@DrawableRes turnIconForkLeft: Int): Builder =
            apply { this.turnIconForkLeft = turnIconForkLeft }

        /**
         * apply fork right icon to the builder.
         * @param turnIconForkRight Int
         * @return Builder
         */
        fun turnIconForkRight(@DrawableRes turnIconForkRight: Int): Builder =
            apply { this.turnIconForkRight = turnIconForkRight }

        /**
         * apply fork straight icon to the builder.
         * @param turnIconForkStraight Int
         * @return Builder
         */
        fun turnIconForkStraight(@DrawableRes turnIconForkStraight: Int): Builder =
            apply { this.turnIconForkStraight = turnIconForkStraight }

        /**
         * apply fork slight left icon to the builder.
         * @param turnIconForkSlightLeft Int
         * @return Builder
         */
        fun turnIconForkSlightLeft(@DrawableRes turnIconForkSlightLeft: Int): Builder =
            apply { this.turnIconForkSlightLeft = turnIconForkSlightLeft }

        /**
         * apply fork slight right icon to the builder.
         * @param turnIconForkSlightRight Int
         * @return Builder
         */
        fun turnIconForkSlightRight(@DrawableRes turnIconForkSlightRight: Int): Builder =
            apply { this.turnIconForkSlightRight = turnIconForkSlightRight }

        /**
         * apply invalid icon to the builder.
         * @param turnIconInvalid Int
         * @return Builder
         */
        fun turnIconInvalid(@DrawableRes turnIconInvalid: Int): Builder =
            apply { this.turnIconInvalid = turnIconInvalid }

        /**
         * apply invalid left icon to the builder.
         * @param turnIconInvalidLeft Int
         * @return Builder
         */
        fun turnIconInvalidLeft(@DrawableRes turnIconInvalidLeft: Int): Builder =
            apply { this.turnIconInvalidLeft = turnIconInvalidLeft }

        /**
         * apply invalid right icon to the builder.
         * @param turnIconInvalidRight Int
         * @return Builder
         */
        fun turnIconInvalidRight(@DrawableRes turnIconInvalidRight: Int): Builder =
            apply { this.turnIconInvalidRight = turnIconInvalidRight }

        /**
         * apply invalid straight icon to the builder.
         * @param turnIconInvalidStraight Int
         * @return Builder
         */
        fun turnIconInvalidStraight(@DrawableRes turnIconInvalidStraight: Int): Builder =
            apply { this.turnIconInvalidStraight = turnIconInvalidStraight }

        /**
         * apply invalid uturn icon to the builder.
         * @param turnIconInvalidUturn Int
         * @return Builder
         */
        fun turnIconInvalidUturn(@DrawableRes turnIconInvalidUturn: Int): Builder =
            apply { this.turnIconInvalidUturn = turnIconInvalidUturn }

        /**
         * apply invalid slight left icon to the builder.
         * @param turnIconInvalidSlightLeft Int
         * @return Builder
         */
        fun turnIconInvalidSlightLeft(@DrawableRes turnIconInvalidSlightLeft: Int): Builder =
            apply { this.turnIconInvalidSlightLeft = turnIconInvalidSlightLeft }

        /**
         * apply invalid slight right icon to the builder.
         * @param turnIconInvalidSlightRight Int
         * @return Builder
         */
        fun turnIconInvalidSlightRight(@DrawableRes turnIconInvalidSlightRight: Int): Builder =
            apply { this.turnIconInvalidSlightRight = turnIconInvalidSlightRight }

        /**
         * apply merge left icon to the builder.
         * @param turnIconMergeLeft Int
         * @return Builder
         */
        fun turnIconMergeLeft(@DrawableRes turnIconMergeLeft: Int): Builder =
            apply { this.turnIconMergeLeft = turnIconMergeLeft }

        /**
         * apply merge right icon to the builder.
         * @param turnIconMergeRight Int
         * @return Builder
         */
        fun turnIconMergeRight(@DrawableRes turnIconMergeRight: Int): Builder =
            apply { this.turnIconMergeRight = turnIconMergeRight }

        /**
         * apply merge straight icon to the builder.
         * @param turnIconMergeStraight Int
         * @return Builder
         */
        fun turnIconMergeStraight(@DrawableRes turnIconMergeStraight: Int): Builder =
            apply { this.turnIconMergeStraight = turnIconMergeStraight }

        /**
         * apply merge slight left icon to the builder.
         * @param turnIconMergeSlightLeft Int
         * @return Builder
         */
        fun turnIconMergeSlightLeft(@DrawableRes turnIconMergeSlightLeft: Int): Builder =
            apply { this.turnIconMergeSlightLeft = turnIconMergeSlightLeft }

        /**
         * apply merge slight right icon to the builder.
         * @param turnIconMergeSlightRight Int
         * @return Builder
         */
        fun turnIconMergeSlightRight(@DrawableRes turnIconMergeSlightRight: Int): Builder =
            apply { this.turnIconMergeSlightRight = turnIconMergeSlightRight }

        /**
         * apply turn new name left to the builder.
         * @param turnIconNewNameLeft Int
         * @return Builder
         */
        fun turnIconNewNameLeft(@DrawableRes turnIconNewNameLeft: Int): Builder =
            apply { this.turnIconNewNameLeft = turnIconNewNameLeft }

        /**
         * apply new name right icon to the builder.
         * @param turnIconNewNameRight Int
         * @return Builder
         */
        fun turnIconNewNameRight(@DrawableRes turnIconNewNameRight: Int): Builder =
            apply { this.turnIconNewNameRight = turnIconNewNameRight }

        /**
         * apply new name straight icon to the builder.
         * @param turnIconNewNameStraight Int
         * @return Builder
         */
        fun turnIconNewNameStraight(@DrawableRes turnIconNewNameStraight: Int): Builder =
            apply { this.turnIconNewNameStraight = turnIconNewNameStraight }

        /**
         * apply new name slight left icon to the builder.
         * @param turnIconNewNameSlightLeft Int
         * @return Builder
         */
        fun turnIconNewNameSlightLeft(@DrawableRes turnIconNewNameSlightLeft: Int): Builder =
            apply { this.turnIconNewNameSlightLeft = turnIconNewNameSlightLeft }

        /**
         * apply new name slight right icon to the builder.
         * @param turnIconNewNameSlightRight Int
         * @return Builder
         */
        fun turnIconNewNameSlightRight(@DrawableRes turnIconNewNameSlightRight: Int): Builder =
            apply { this.turnIconNewNameSlightRight = turnIconNewNameSlightRight }

        /**
         * apply new name sharp left icon to the builder.
         * @param turnIconNewNameSharpLeft Int
         * @return Builder
         */
        fun turnIconNewNameSharpLeft(@DrawableRes turnIconNewNameSharpLeft: Int): Builder =
            apply { this.turnIconNewNameSharpLeft = turnIconNewNameSharpLeft }

        /**
         * apply new name sharp right icon to the builder.
         * @param turnIconNewNameSharpRight Int
         * @return Builder
         */
        fun turnIconNewNameSharpRight(@DrawableRes turnIconNewNameSharpRight: Int): Builder =
            apply { this.turnIconNewNameSharpRight = turnIconNewNameSharpRight }

        /**
         * apply notification left icon to the builder.
         * @param turnIconNotificationLeft Int
         * @return Builder
         */
        fun turnIconNotificationLeft(@DrawableRes turnIconNotificationLeft: Int): Builder =
            apply { this.turnIconNotificationLeft = turnIconNotificationLeft }

        /**
         * apply notification right icon to the builder.
         * @param turnIconNotificationRight Int
         * @return Builder
         */
        fun turnIconNotificationRight(@DrawableRes turnIconNotificationRight: Int): Builder =
            apply { this.turnIconNotificationRight = turnIconNotificationRight }

        /**
         * apply notification straight icon to the builder.
         * @param turnIconNotificationStraight Int
         * @return Builder
         */
        fun turnIconNotificationStraight(
            @DrawableRes turnIconNotificationStraight: Int,
        ): Builder =
            apply { this.turnIconNotificationStraight = turnIconNotificationStraight }

        /**
         * apply notification slight left icon to the builder.
         * @param turnIconNotificationSlightLeft Int
         * @return Builder
         */
        fun turnIconNotificationSlightLeft(
            @DrawableRes turnIconNotificationSlightLeft: Int,
        ): Builder =
            apply { this.turnIconNotificationSlightLeft = turnIconNotificationSlightLeft }

        /**
         * apply notification slight right icon to the builder.
         * @param turnIconNotificationSlightRight Int
         * @return Builder
         */
        fun turnIconNotificationSlightRight(
            @DrawableRes turnIconNotificationSlightRight: Int,
        ): Builder =
            apply { this.turnIconNotificationSlightRight = turnIconNotificationSlightRight }

        /**
         * apply notification sharp left icon to the builder.
         * @param turnIconNotificationSharpLeft Int
         * @return Builder
         */
        fun turnIconNotificationSharpLeft(
            @DrawableRes turnIconNotificationSharpLeft: Int,
        ): Builder =
            apply { this.turnIconNotificationSharpLeft = turnIconNotificationSharpLeft }

        /**
         * apply notification sharp right icon to the builder.
         * @param turnIconNotificationSharpRight Int
         * @return Builder
         */
        fun turnIconNotificationSharpRight(
            @DrawableRes turnIconNotificationSharpRight: Int,
        ): Builder =
            apply { this.turnIconNotificationSharpRight = turnIconNotificationSharpRight }

        /**
         * apply off ramp icon to the builder.
         * @param turnIconOffRamp Int
         * @return Builder
         */
        fun turnIconOffRamp(@DrawableRes turnIconOffRamp: Int): Builder =
            apply { this.turnIconOffRamp = turnIconOffRamp }

        /**
         * apply off ramp left icon to the builder.
         * @param turnIconOffRampLeft Int
         * @return Builder
         */
        fun turnIconOffRampLeft(@DrawableRes turnIconOffRampLeft: Int): Builder =
            apply { this.turnIconOffRampLeft = turnIconOffRampLeft }

        /**
         * apply off ramp right icon to the builder.
         * @param turnIconOffRampRight Int
         * @return Builder
         */
        fun turnIconOffRampRight(@DrawableRes turnIconOffRampRight: Int): Builder =
            apply { this.turnIconOffRampRight = turnIconOffRampRight }

        /**
         * apply off ramp slight left icon to the builder.
         * @param turnIconOffRampSlightLeft Int
         * @return Builder
         */
        fun turnIconOffRampSlightLeft(@DrawableRes turnIconOffRampSlightLeft: Int): Builder =
            apply { this.turnIconOffRampSlightLeft = turnIconOffRampSlightLeft }

        /**
         * apply off ramp slight right icon to the builder.
         * @param turnIconOffRampSlightRight Int
         * @return Builder
         */
        fun turnIconOffRampSlightRight(@DrawableRes turnIconOffRampSlightRight: Int): Builder =
            apply { this.turnIconOffRampSlightRight = turnIconOffRampSlightRight }

        /**
         * apply on ramp icon to the builder.
         * @param turnIconOnRamp Int
         * @return Builder
         */
        fun turnIconOnRamp(@DrawableRes turnIconOnRamp: Int): Builder =
            apply { this.turnIconOnRamp = turnIconOnRamp }

        /**
         * apply on ramp left icon to the builder.
         * @param turnIconOnRampLeft Int
         * @return Builder
         */
        fun turnIconOnRampLeft(@DrawableRes turnIconOnRampLeft: Int): Builder =
            apply { this.turnIconOnRampLeft = turnIconOnRampLeft }

        /**
         * apply on ramp right icon to the builder.
         * @param turnIconOnRampRight Int
         * @return Builder
         */
        fun turnIconOnRampRight(@DrawableRes turnIconOnRampRight: Int): Builder =
            apply { this.turnIconOnRampRight = turnIconOnRampRight }

        /**
         * apply on ramp straight icon to the builder.
         * @param turnIconOnRampStraight Int
         * @return Builder
         */
        fun turnIconOnRampStraight(@DrawableRes turnIconOnRampStraight: Int): Builder =
            apply { this.turnIconOnRampStraight = turnIconOnRampStraight }

        /**
         * apply on ramp slight left icon to the builder.
         * @param turnIconOnRampSlightLeft Int
         * @return Builder
         */
        fun turnIconOnRampSlightLeft(@DrawableRes turnIconOnRampSlightLeft: Int): Builder =
            apply { this.turnIconOnRampSlightLeft = turnIconOnRampSlightLeft }

        /**
         * apply on ramp slight right icon to the builder.
         * @param turnIconOnRampSlightRight Int
         * @return Builder
         */
        fun turnIconOnRampSlightRight(@DrawableRes turnIconOnRampSlightRight: Int): Builder =
            apply { this.turnIconOnRampSlightRight = turnIconOnRampSlightRight }

        /**
         * apply on ramp sharp left icon to the builder.
         * @param turnIconOnRampSharpLeft Int
         * @return Builder
         */
        fun turnIconOnRampSharpLeft(@DrawableRes turnIconOnRampSharpLeft: Int): Builder =
            apply { this.turnIconOnRampSharpLeft = turnIconOnRampSharpLeft }

        /**
         * apply on ramp sharp right icon to the builder.
         * @param turnIconOnRampSharpRight Int
         * @return Builder
         */
        fun turnIconOnRampSharpRight(@DrawableRes turnIconOnRampSharpRight: Int): Builder =
            apply { this.turnIconOnRampSharpRight = turnIconOnRampSharpRight }

        /**
         * apply ramp icon to the builder.
         * @param turnIconRamp Int
         * @return Builder
         */
        fun turnIconRamp(@DrawableRes turnIconRamp: Int): Builder =
            apply { this.turnIconRamp = turnIconRamp }

        /**
         * apply rotary icon to the builder.
         * @param turnIconRotary Int
         * @return Builder
         */
        fun turnIconRotary(@DrawableRes turnIconRotary: Int): Builder =
            apply { this.turnIconRotary = turnIconRotary }

        /**
         * apply rotary left icon to the builder.
         * @param turnIconRotaryLeft Int
         * @return Builder
         */
        fun turnIconRotaryLeft(@DrawableRes turnIconRotaryLeft: Int): Builder =
            apply { this.turnIconRotaryLeft = turnIconRotaryLeft }

        /**
         * apply rotary right icon to the builder.
         * @param turnIconRotaryRight Int
         * @return Builder
         */
        fun turnIconRotaryRight(@DrawableRes turnIconRotaryRight: Int): Builder =
            apply { this.turnIconRotaryRight = turnIconRotaryRight }

        /**
         * apply rotary straight icon to the builder.
         * @param turnIconRotaryStraight Int
         * @return Builder
         */
        fun turnIconRotaryStraight(@DrawableRes turnIconRotaryStraight: Int): Builder =
            apply { this.turnIconRotaryStraight = turnIconRotaryStraight }

        /**
         * apply rotary slight left icon to the builder.
         * @param turnIconRotarySlightLeft Int
         * @return Builder
         */
        fun turnIconRotarySlightLeft(@DrawableRes turnIconRotarySlightLeft: Int): Builder =
            apply { this.turnIconRotarySlightLeft = turnIconRotarySlightLeft }

        /**
         * apply rotary slight right icon to the builder.
         * @param turnIconRotarySlightRight Int
         * @return Builder
         */
        fun turnIconRotarySlightRight(@DrawableRes turnIconRotarySlightRight: Int): Builder =
            apply { this.turnIconRotarySlightRight = turnIconRotarySlightRight }

        /**
         * apply rotary sharp left icon to the builder.
         * @param turnIconRotarySharpLeft Int
         * @return Builder
         */
        fun turnIconRotarySharpLeft(@DrawableRes turnIconRotarySharpLeft: Int): Builder =
            apply { this.turnIconRotarySharpLeft = turnIconRotarySharpLeft }

        /**
         * apply rotary sharp right icon to the builder.
         * @param turnIconRotarySharpRight Int
         * @return Builder
         */
        fun turnIconRotarySharpRight(@DrawableRes turnIconRotarySharpRight: Int): Builder =
            apply { this.turnIconRotarySharpRight = turnIconRotarySharpRight }

        /**
         * apply roundabout icon to the builder.
         * @param turnIconRoundabout Int
         * @return Builder
         */
        fun turnIconRoundabout(@DrawableRes turnIconRoundabout: Int): Builder =
            apply { this.turnIconRoundabout = turnIconRoundabout }

        /**
         * apply roundabout left icon to the builder.
         * @param turnIconRoundaboutLeft Int
         * @return Builder
         */
        fun turnIconRoundaboutLeft(@DrawableRes turnIconRoundaboutLeft: Int): Builder =
            apply { this.turnIconRoundaboutLeft = turnIconRoundaboutLeft }

        /**
         * apply roundabout right icon to the builder.
         * @param turnIconRoundaboutRight Int
         * @return Builder
         */
        fun turnIconRoundaboutRight(@DrawableRes turnIconRoundaboutRight: Int): Builder =
            apply { this.turnIconRoundaboutRight = turnIconRoundaboutRight }

        /**
         * apply roundabout straight icon to the builder.
         * @param turnIconRoundaboutStraight Int
         * @return Builder
         */
        fun turnIconRoundaboutStraight(@DrawableRes turnIconRoundaboutStraight: Int): Builder =
            apply { this.turnIconRoundaboutStraight = turnIconRoundaboutStraight }

        /**
         * apply roundabout slight left icon to the builder.
         * @param turnIconRoundaboutSlightLeft Int
         * @return Builder
         */
        fun turnIconRoundaboutSlightLeft(
            @DrawableRes turnIconRoundaboutSlightLeft: Int,
        ): Builder =
            apply { this.turnIconRoundaboutSlightLeft = turnIconRoundaboutSlightLeft }

        /**
         * apply roundabout slight right icon to the builder.
         * @param turnIconRoundaboutSlightRight Int
         * @return Builder
         */
        fun turnIconRoundaboutSlightRight(
            @DrawableRes turnIconRoundaboutSlightRight: Int,
        ): Builder =
            apply { this.turnIconRoundaboutSlightRight = turnIconRoundaboutSlightRight }

        /**
         * apply roundabout sharp left icon to the builder.
         * @param turnIconRoundaboutSharpLeft Int
         * @return Builder
         */
        fun turnIconRoundaboutSharpLeft(@DrawableRes turnIconRoundaboutSharpLeft: Int): Builder =
            apply { this.turnIconRoundaboutSharpLeft = turnIconRoundaboutSharpLeft }

        /**
         * apply roundabout sharp right icon to the builder.
         * @param turnIconRoundaboutSharpRight Int
         * @return Builder
         */
        fun turnIconRoundaboutSharpRight(
            @DrawableRes turnIconRoundaboutSharpRight: Int,
        ): Builder =
            apply { this.turnIconRoundaboutSharpRight = turnIconRoundaboutSharpRight }

        /**
         * apply turn left icon to the builder.
         * @param turnIconTurnLeft Int
         * @return Builder
         */
        fun turnIconTurnLeft(@DrawableRes turnIconTurnLeft: Int): Builder =
            apply { this.turnIconTurnLeft = turnIconTurnLeft }

        /**
         * apply turn right icon to the builder.
         * @param turnIconTurnRight Int
         * @return Builder
         */
        fun turnIconTurnRight(@DrawableRes turnIconTurnRight: Int): Builder =
            apply { this.turnIconTurnRight = turnIconTurnRight }

        /**
         * apply turn straight icon to the builder.
         * @param turnIconTurnStraight Int
         * @return Builder
         */
        fun turnIconTurnStraight(@DrawableRes turnIconTurnStraight: Int): Builder =
            apply { this.turnIconTurnStraight = turnIconTurnStraight }

        /**
         * apply turn slight left icon to the builder.
         * @param turnIconTurnSlightLeft Int
         * @return Builder
         */
        fun turnIconTurnSlightLeft(@DrawableRes turnIconTurnSlightLeft: Int): Builder =
            apply { this.turnIconTurnSlightLeft = turnIconTurnSlightLeft }

        /**
         * apply turn slight right icon to the builder.
         * @param turnIconTurnSlightRight Int
         * @return Builder
         */
        fun turnIconTurnSlightRight(@DrawableRes turnIconTurnSlightRight: Int): Builder =
            apply { this.turnIconTurnSlightRight = turnIconTurnSlightRight }

        /**
         * apply turn sharp left icon to the builder.
         * @param turnIconTurnSharpLeft Int
         * @return Builder
         */
        fun turnIconTurnSharpLeft(@DrawableRes turnIconTurnSharpLeft: Int): Builder =
            apply { this.turnIconTurnSharpLeft = turnIconTurnSharpLeft }

        /**
         * apply turn sharp right icon to the builder.
         * @param turnIconTurnSharpRight Int
         * @return Builder
         */
        fun turnIconTurnSharpRight(@DrawableRes turnIconTurnSharpRight: Int): Builder =
            apply { this.turnIconTurnSharpRight = turnIconTurnSharpRight }

        /**
         * apply uturn icon to the builder.
         * @param turnIconUturn Int
         * @return Builder
         */
        fun turnIconUturn(@DrawableRes turnIconUturn: Int): Builder =
            apply { this.turnIconUturn = turnIconUturn }

        /**
         * Build the [NotificationTurnIconResources].
         * @return NotificationTurnIconResources
         */
        fun build(): NotificationTurnIconResources {
            return NotificationTurnIconResources(
                turnIconArrive,
                turnIconArriveLeft,
                turnIconArriveRight,
                turnIconArriveStraight,
                turnIconContinue,
                turnIconContinueLeft,
                turnIconContinueRight,
                turnIconContinueStraight,
                turnIconContinueUturn,
                turnIconContinueSlightLeft,
                turnIconContinueSlightRight,
                turnIconDepart,
                turnIconDepartLeft,
                turnIconDepartRight,
                turnIconDepartStraight,
                turnIconEndRoadLeft,
                turnIconEndRoadRight,
                turnIconFork,
                turnIconForkLeft,
                turnIconForkRight,
                turnIconForkStraight,
                turnIconForkSlightLeft,
                turnIconForkSlightRight,
                turnIconInvalid,
                turnIconInvalidLeft,
                turnIconInvalidRight,
                turnIconInvalidStraight,
                turnIconInvalidSlightLeft,
                turnIconInvalidSlightRight,
                turnIconInvalidUturn,
                turnIconMergeLeft,
                turnIconMergeRight,
                turnIconMergeStraight,
                turnIconMergeSlightLeft,
                turnIconMergeSlightRight,
                turnIconNewNameLeft,
                turnIconNewNameRight,
                turnIconNewNameStraight,
                turnIconNewNameSharpLeft,
                turnIconNewNameSharpRight,
                turnIconNewNameSlightLeft,
                turnIconNewNameSlightRight,
                turnIconNotificationLeft,
                turnIconNotificationRight,
                turnIconNotificationStraight,
                turnIconNotificationSharpLeft,
                turnIconNotificationSharpRight,
                turnIconNotificationSlightLeft,
                turnIconNotificationSlightRight,
                turnIconOffRamp,
                turnIconOffRampLeft,
                turnIconOffRampRight,
                turnIconOffRampSlightLeft,
                turnIconOffRampSlightRight,
                turnIconOnRamp,
                turnIconOnRampLeft,
                turnIconOnRampRight,
                turnIconOnRampStraight,
                turnIconOnRampSlightLeft,
                turnIconOnRampSlightRight,
                turnIconOnRampSharpLeft,
                turnIconOnRampSharpRight,
                turnIconRamp,
                turnIconRotary,
                turnIconRotaryLeft,
                turnIconRotaryRight,
                turnIconRotaryStraight,
                turnIconRotarySlightLeft,
                turnIconRotarySlightRight,
                turnIconRotarySharpLeft,
                turnIconRotarySharpRight,
                turnIconRoundabout,
                turnIconRoundaboutLeft,
                turnIconRoundaboutRight,
                turnIconRoundaboutStraight,
                turnIconRoundaboutSlightLeft,
                turnIconRoundaboutSlightRight,
                turnIconRoundaboutSharpLeft,
                turnIconRoundaboutSharpRight,
                turnIconTurnLeft,
                turnIconTurnRight,
                turnIconTurnStraight,
                turnIconTurnSlightLeft,
                turnIconTurnSlightRight,
                turnIconTurnSharpLeft,
                turnIconTurnSharpRight,
                turnIconUturn,
            )
        }
    }
}

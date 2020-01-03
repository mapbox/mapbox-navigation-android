package com.mapbox.navigation.base.route.model

import androidx.annotation.StringDef

const val TURN = "turn"
const val NEW_NAME = "new name"
const val DEPART = "depart"
const val ARRIVE = "arrive"
const val MERGE = "merge"
const val ON_RAMP = "on ramp"
const val OFF_RAMP = "off ramp"
const val FORK = "fork"
const val END_OF_ROAD = "end of road"
const val CONTINUE = "continue"
const val ROUNDABOUT = "roundabout"
const val ROTARY = "rotary"
const val ROUNDABOUT_TURN = "roundabout turn"
const val NOTIFICATION = "notification"
const val EXIT_ROUNDABOUT = "exit roundabout"
const val EXIT_ROTARY = "exit rotary"

@Retention(AnnotationRetention.SOURCE)
@StringDef(TURN,
        NEW_NAME,
        DEPART,
        ARRIVE,
        MERGE,
        ON_RAMP,
        OFF_RAMP,
        FORK,
        END_OF_ROAD,
        CONTINUE,
        ROUNDABOUT,
        ROTARY,
        ROUNDABOUT_TURN,
        NOTIFICATION,
        EXIT_ROUNDABOUT,
        EXIT_ROTARY)
annotation class StepManeuverType
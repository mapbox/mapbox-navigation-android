package com.mapbox.navigation.copilot

internal sealed class DriveEndsType {

    abstract val type: String

    object ApplicationClosed : DriveEndsType() {

        override val type = "application_closed"
    }

    object VehicleParked : DriveEndsType() {

        override val type = "vehicle_parked"
    }

    object CanceledManually : DriveEndsType() {

        override val type = "canceled_manually"
    }

    object Arrived : DriveEndsType() {

        override val type = "arrived"
    }
}

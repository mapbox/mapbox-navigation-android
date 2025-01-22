package com.mapbox.navigation.core.accounts

import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.SdkInformation
import com.mapbox.common.SessionSKUIdentifier
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.accounts.SkuIdProvider
import com.mapbox.navigation.base.internal.accounts.SkuIdProviderImpl
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigator.Waypoint
import com.mapbox.navigator.WaypointType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BillingControllerWaypointsComparisonTest(
    private val description: String,
    private val remainingWaypoints: Int,
    private val initialLegIndex: Int,
    private val oldWaypoints: List<Waypoint>,
    private val newWaypoints: List<Waypoint>,
    private val shouldStartSession: Boolean,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(
                    "Route start, regular + regular with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, regular + user EV with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, regular + EV with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_SERVER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, regular and user EV + EV with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_SERVER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, EV + regular with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, EV + regular and user EV with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, EV + EV with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, EV and user EV + EV with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, regular + regular with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, regular + user EV with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, regular + EV with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_SERVER),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, regular and user EV + EV " +
                        "with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_SERVER),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, EV + regular with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, EV + regular and EV " +
                        "with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, EV + EV with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, EV + EV and user EV " +
                        "with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(6.6, 7.7, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, regular and silent + regular with matching size " +
                        "and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, regular + regular with non-zero leg index - sizes don't match",
                    2,
                    1,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, regular and silent + regular with non-zero leg index - " +
                        "sizes don't match",
                    2,
                    1,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, regular + regular and silent with non-zero leg index - " +
                        "sizes don't match",
                    2,
                    1,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, regular + regular with non-zero leg index and prev silent - " +
                        "sizes don't match",
                    2,
                    1,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(1.1, 2.2, WaypointType.SILENT),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route start, regular + regular and silent with non-zero leg index " +
                        "and prev silent - sizes match",
                    2,
                    1,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(1.1, 2.3, WaypointType.SILENT),
                        waypoint(1.1, 2.4, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, regular + regular and silent with matching size " +
                        "and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, regular + regular with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, regular and user EV + regular " +
                        "with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, regular and user EV + regular and silent " +
                        "with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, regular + EV with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, regular and silent + EV with matching size " +
                        "and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, regular and user EV + EV " +
                        "with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, regular and user EV + EV and silent " +
                        "with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, EV + regular with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, EV + regular and silent with matching size " +
                        "and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, EV + regular and user EV " +
                        "with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, EV and silent + regular and user EV " +
                        "with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, EV + EV with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, EV + EV and silent with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, EV and user EV + EV with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, EV and user EV and silent + EV with matching size " +
                        "and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.SILENT),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, has waypoints + empty waypoints",
                    1,
                    0,
                    listOf(
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    emptyList<Waypoint>(),
                    true,
                ),
                arrayOf(
                    "Route start, invalid leg index - sizes match",
                    1,
                    2,
                    listOf(
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route start, invalid leg index - sizes don't match",
                    1,
                    3,
                    listOf(
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),

                arrayOf(
                    "Route middle, regular + regular with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, regular and user EV + regular with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.EV_CHARGING_USER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, regular + EV with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, regular + EV and user EV with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, EV + regular with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, EV and user EV + regular with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.EV_CHARGING_USER),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, EV + EV with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, EV and user EV + EV with non-matching size",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, regular + regular with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, regular and user EV + regular " +
                        "with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, regular + EV with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, regular and user EV + EV " +
                        "with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.EV_CHARGING_USER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, EV + regular with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, EV and user EV + regular " +
                        "with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, EV + EV with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, EV and user EV + EV " +
                        "with matching size and non-matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.EV_CHARGING_USER),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, regular + regular with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, regular + regular and silent with matching size " +
                        "and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, regular and user EV + regular " +
                        "with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, regular and user EV + regular and silent " +
                        "with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, regular + EV with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, regular and silent + EV with matching size " +
                        "and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, regular + EV and user EV " +
                        "with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, regular and silent + EV and user EV " +
                        "with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, EV + regular with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, EV + regular and silent with matching size " +
                        "and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, EV + regular and user EV " +
                        "with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, EV and silent + regular and user EV " +
                        "with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, EV + EV with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, EV + EV and silent with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, EV + EV and user EV with matching size and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, EV and silent + EV and user EV with matching size " +
                        "and matching locations",
                    2,
                    0,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, EV + EV and user EV and silent with non-zero leg index - " +
                        "sizes don't match",
                    2,
                    1,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_SERVER),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, EV + EV and user EV and silent with non-zero leg index - " +
                        "sizes match",
                    2,
                    1,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    false,
                ),
                arrayOf(
                    "Route middle, EV + EV and user EV and silent with non-zero leg index " +
                        "and prev silent - sizes don't match",
                    2,
                    1,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(1.1, 2.3, WaypointType.SILENT),
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.EV_CHARGING_SERVER),
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    true,
                ),
                arrayOf(
                    "Route middle, EV + EV and user EV and silent with non-zero leg index " +
                        "and prev silent - sizes match",
                    2,
                    1,
                    listOf(
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(5.5, 6.6, WaypointType.EV_CHARGING_SERVER),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(1.1, 2.3, WaypointType.SILENT),
                        waypoint(2.2, 3.3, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    false,
                ),

                arrayOf(
                    "Route finished, regular + regular with matching size and matching locations",
                    0,
                    0,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route finished, regular and user EV + regular " +
                        "with matching size and matching locations",
                    0,
                    0,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.EV_CHARGING_USER),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route not started, regular + regular " +
                        "with matching size and matching locations",
                    3,
                    0,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route not started, regular + regular and silent " +
                        "with matching size and matching locations",
                    3,
                    0,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route not started, regular and user EV + regular " +
                        "with matching size and matching locations",
                    3,
                    0,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route not started, regular and user EV + regular and silent " +
                        "with matching size and matching locations",
                    3,
                    0,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route not started, regular + regular with matching size " +
                        "and matching locations but different origin",
                    3,
                    0,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route not started, regular + regular and silent with matching size " +
                        "and matching locations but different origin",
                    3,
                    0,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    listOf(
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route not started, regular and user EV + regular with matching size " +
                        "and matching locations but different origin",
                    3,
                    0,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route not started, regular and user EV + regular and silent " +
                        "with matching size and matching locations but different origin",
                    3,
                    0,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.SILENT),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route not started, regular and user EV + regular and silent with non-zero " +
                        "leg index - sizes don't match",
                    3,
                    1,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route not started, regular and user EV + regular and silent with non-zero " +
                        "leg index - sizes match",
                    3,
                    1,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
                arrayOf(
                    "Route not started, regular and user EV + regular and silent with non-zero " +
                        "leg index and prev silent - sizes don't match",
                    3,
                    1,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.SILENT),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    true,
                ),
                arrayOf(
                    "Route not started, regular and user EV + regular and silent with non-zero " +
                        "leg index and prev silent - sizes match",
                    3,
                    1,
                    listOf(
                        waypoint(1.1, 2.2, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.EV_CHARGING_USER),
                    ),
                    listOf(
                        waypoint(5.5, 6.6, WaypointType.REGULAR),
                        waypoint(6.6, 7.7, WaypointType.SILENT),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(3.3, 4.4, WaypointType.REGULAR),
                        waypoint(4.4, 5.5, WaypointType.REGULAR),
                    ),
                    false,
                ),
            )
        }

        private fun waypoint(
            longitude: Double,
            latitude: Double,
            waypointType: WaypointType,
        ): Waypoint {
            return mockk(relaxed = true) {
                every { location } returns Point.fromLngLat(longitude, latitude)
                every { type } returns waypointType
            }
        }
    }

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var navigationSession: NavigationSession
    private lateinit var tripSession: TripSession
    private lateinit var arrivalProgressObserver: ArrivalProgressObserver
    private lateinit var skuIdProvider: SkuIdProvider
    private lateinit var sdkInformation: SdkInformation
    private val billingService = mockk<BillingServiceProxy>(relaxed = true)
    private lateinit var routeProgress: RouteProgress
    private lateinit var newRoute: NavigationRoute

    private lateinit var billingController: BillingController

    @Before
    fun setup() {
        mockkObject(BillingServiceProvider)
        every { BillingServiceProvider.getInstance() } returns billingService

        every {
            billingService.getSessionStatus(any())
        } returns BillingSessionStatus.NO_SESSION
        every {
            billingService.getSessionStatus(SessionSKUIdentifier.NAV3_SES_CORE_AGTRIP)
        } returns BillingSessionStatus.SESSION_ACTIVE

        val sessionStateObserverSlot = slot<NavigationSessionStateObserver>()
        navigationSession = mockk(relaxUnitFun = true) {
            every {
                registerNavigationSessionStateObserver(capture(sessionStateObserverSlot))
            } just Runs
        }

        arrivalProgressObserver = mockk(relaxUnitFun = true)
        routeProgress = mockk(relaxed = true) {
            every { navigationRoute } returns createNavigationRoute(
                directionsRoute = createDirectionsRoute(),
                nativeWaypoints = oldWaypoints,
            )
            every {
                remainingWaypoints
            } returns this@BillingControllerWaypointsComparisonTest.remainingWaypoints
        }
        newRoute = createNavigationRoute(
            directionsRoute = createDirectionsRoute(),
            nativeWaypoints = newWaypoints,
        )

        tripSession = mockk(relaxed = true) {
            every { getRouteProgress() } returns routeProgress
        }

        skuIdProvider = SkuIdProviderImpl()

        sdkInformation = SdkInformation("test-name", "test-version", "test-package")

        billingController = BillingController(
            navigationSession,
            arrivalProgressObserver,
            tripSession,
            skuIdProvider,
            sdkInformation,
        )
    }

    @Test
    fun compareWaypoints() {
        billingController.onExternalRouteSet(newRoute, initialLegIndex)

        verify(exactly = if (shouldStartSession) 1 else 0) {
            billingService.beginBillingSession(any(), any(), any(), any())
        }
    }
}

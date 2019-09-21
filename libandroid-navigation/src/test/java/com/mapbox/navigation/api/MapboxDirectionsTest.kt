package com.mapbox.navigation.api

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import java.lang.IllegalStateException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class MapboxDirectionsTest {
  private var directions: Directions = mock(Directions::class.java)
  private lateinit var directionsSession: DirectionsSession
  private var directionsRoute: Array<DirectionsRoute> = arrayOf()
  private var latch: CountDownLatch? = null
  private var listener: DirectionsSession.Listener = DirectionsSession.Listener { directionsRoute: Array<DirectionsRoute> ->
    this.directionsRoute = directionsRoute
    this.latch?.countDown()
  }

  private var fasterRouteListener: DirectionsSession.Listener = DirectionsSession.Listener { fasterRoute: Array<DirectionsRoute> ->
    // TODO: faster route available
  }

  @Test
  fun getDirections() {
    // FIXME: replace with route request object
    val routeRequest = NavigationRoute.builder(mock(Context::class.java)).build()
    directionsSession = directions.getDirections(routeRequest, listener)
    latch = CountDownLatch(1)
    assertThat(latch?.await(1, TimeUnit.SECONDS)).isTrue()
    assertThat(directionsSession.route).isSameAs(directionsRoute.first())
  }

  @Test(expected = IllegalStateException::class)
  fun cancelDirectionsRequest() {
    val routeRequest = NavigationRoute.builder(mock(Context::class.java)).build()
    directionsSession = directions.getDirections(routeRequest, listener)
    directionsSession.cancel()
    directionsSession.addWaypoint(Point.fromJson(""))
  }

  @Test
  fun registerFasterRouteListener() {
    val routeRequest = NavigationRoute.builder(mock(Context::class.java)).build()
    directionsSession = directions.getDirections(routeRequest, listener)
    directionsSession.setFasterRouteListener(fasterRouteListener)
  }

  @Test
  fun addWaypoint() {
    val routeRequest = NavigationRoute.builder(mock(Context::class.java)).build()
    directionsSession = directions.getDirections(routeRequest, listener)
    directionsSession.addWaypoint(Point.fromJson(""))
    latch = CountDownLatch(1)
    assertThat(latch?.await(1, TimeUnit.SECONDS)).isTrue()
    assertThat(directionsSession.route).isSameAs(directionsRoute.first())
  }

  @Test
  fun removeWaypoint() {
    val routeRequest = NavigationRoute.builder(mock(Context::class.java)).build()
    directionsSession = directions.getDirections(routeRequest, listener)
    directionsSession.addWaypoint(Point.fromJson(""))
    latch = CountDownLatch(1)
    assertThat(latch?.await(1, TimeUnit.SECONDS)).isTrue()

    directionsSession.removeWaypoint(Point.fromJson(""))
    latch = CountDownLatch(1)
    assertThat(latch?.await(1, TimeUnit.SECONDS)).isTrue()
  }

  @Test
  fun reRoute() {
    val routeRequest = NavigationRoute.builder(mock(Context::class.java)).build()
    directionsSession = directions.getDirections(routeRequest, listener)
    directionsSession.reRoute(Point.fromJson(""))
    latch = CountDownLatch(1)
    assertThat(latch?.await(1, TimeUnit.SECONDS)).isTrue()
    assertThat(directionsSession.route).isSameAs(directionsRoute.first())
  }

  @Test
  fun setProfile() {
    val routeRequest = NavigationRoute.builder(mock(Context::class.java)).build()
    directionsSession = directions.getDirections(routeRequest, listener)
    directionsSession.setProfile(DirectionsCriteria.PROFILE_CYCLING)
    latch = CountDownLatch(1)
    assertThat(latch?.await(1, TimeUnit.SECONDS)).isTrue()
    assertThat(directionsSession.route).isSameAs(directionsRoute.first())
  }
}
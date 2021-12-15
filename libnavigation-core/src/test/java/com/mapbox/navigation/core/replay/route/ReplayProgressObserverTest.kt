package com.mapbox.navigation.core.replay.route

import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

class ReplayProgressObserverTest {
    private val replayer = mockk<MapboxReplayer>(relaxUnitFun = true) {
        every { pushEvents(any()) } returns this
    }
    private val mapper = mockk<ReplayRouteMapper>()

    private val replayProgressObserver = ReplayProgressObserver(replayer, mapper)

    @Test
    fun `old events are cleared when new leg starts`() {
        val oldLeg = mockk<RouteLeg>()
        val oldEvents = listOf<ReplayEventBase>(mockk())
        val newLeg = mockk<RouteLeg>()
        val newEvents = listOf<ReplayEventBase>(mockk())
        every { mapper.mapRouteLegGeometry(oldLeg) } returns oldEvents
        every { mapper.mapRouteLegGeometry(newLeg) } returns newEvents
        every { replayer.isPlaying() } returns true

        // provide old leg
        replayProgressObserver.onRouteProgressChanged(
            mockk {
                every { currentLegProgress } returns mockk {
                    every { routeLeg } returns oldLeg
                }
            }
        )

        // provide new leg
        replayProgressObserver.onRouteProgressChanged(
            mockk {
                every { currentLegProgress } returns mockk {
                    every { routeLeg } returns newLeg
                }
            }
        )

        verifyOrder {
            replayer.clearEvents()
            replayer.pushEvents(oldEvents)
            replayer.seekTo(oldEvents.first())
            replayer.play()
            replayer.clearEvents()
            replayer.pushEvents(newEvents)
            replayer.seekTo(newEvents.first())
            replayer.play()
        }
    }

    @Test
    fun `when new leg starts don't start playing if we were not already playing`() {
        val newLeg = mockk<RouteLeg>()
        val newEvents = listOf<ReplayEventBase>()
        every { mapper.mapRouteLegGeometry(newLeg) } returns newEvents
        every { replayer.isPlaying() } returns false

        // provide new leg
        replayProgressObserver.onRouteProgressChanged(
            mockk {
                every { currentLegProgress } returns mockk {
                    every { routeLeg } returns newLeg
                }
            }
        )

        verify(exactly = 0) {
            replayer.play()
        }
    }
}

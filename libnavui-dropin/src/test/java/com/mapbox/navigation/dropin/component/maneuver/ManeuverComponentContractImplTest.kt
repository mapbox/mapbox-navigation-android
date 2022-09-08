package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverViewState
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
class ManeuverComponentContractImplTest {

    @Test
    fun `when maneuver view state is changed, contract is notified`() {
        val navigationViewContext: NavigationViewContext = mockk(relaxed = true) {
            every { maneuverBehavior.updateBehavior(any()) } just Runs
        }
        val sut = ManeuverComponentContractImpl(navigationViewContext)

        sut.onManeuverViewStateChanged(MapboxManeuverViewState.EXPANDED)

        verify {
            navigationViewContext.maneuverBehavior.updateBehavior(MapboxManeuverViewState.EXPANDED)
        }
    }
}

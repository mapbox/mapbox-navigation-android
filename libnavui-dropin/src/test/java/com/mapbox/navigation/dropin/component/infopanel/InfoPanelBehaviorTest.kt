package com.mapbox.navigation.dropin.component.infopanel

import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.junit.Assert.assertEquals
import org.junit.Test

class InfoPanelBehaviorTest {

    @Test
    fun `when info panel behavior is updated`() {
        val sut = InfoPanelBehavior()

        sut.updateBehavior(BottomSheetBehavior.STATE_HIDDEN)

        assertEquals(BottomSheetBehavior.STATE_HIDDEN, sut.infoPanelBehavior.value)
    }
}

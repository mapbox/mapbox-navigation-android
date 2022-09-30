package com.mapbox.navigation.dropin.infopanel

import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.junit.Assert.assertEquals
import org.junit.Test

class InfoPanelBehaviorTest {

    @Test
    fun `when info panel behavior is updated`() {
        val sut = InfoPanelBehavior()

        sut.updateBottomSheetState(BottomSheetBehavior.STATE_HIDDEN)

        assertEquals(BottomSheetBehavior.STATE_HIDDEN, sut.bottomSheetState.value)
    }
}

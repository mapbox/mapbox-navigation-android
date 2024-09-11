package com.mapbox.navigation.ui.components

import android.R
import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.ui.components.MapboxExtendableButton.State
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MapboxExtendableButtonTest {

    private lateinit var ctx: Context
    private lateinit var sut: MapboxExtendableButton

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        sut = MapboxExtendableButton(ctx, null, 0)
    }

    @Test
    fun `setState should update iconImage`() {
        sut.setState(State(R.drawable.ic_secure))

        assertEquals(
            R.drawable.ic_secure,
            shadowOf(sut.iconImage.drawable).createdFromResId,
        )
    }

    @Test
    fun `setState should update and show TEXT`() {
        sut.setState(State(R.drawable.ic_secure, "text", 1000))

        assertEquals("text", sut.textView.text)
        assertEquals(View.VISIBLE, sut.textView.visibility)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(State::class.java)
            .verify()

        ToStringVerifier.forClass(State::class.java)
            .verify()
    }
}

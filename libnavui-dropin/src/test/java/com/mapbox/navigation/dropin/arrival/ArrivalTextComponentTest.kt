package com.mapbox.navigation.dropin.arrival

import android.content.Context
import android.os.Build
import androidx.appcompat.widget.AppCompatTextView
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class ArrivalTextComponentTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var textView: AppCompatTextView
    private lateinit var textAppearance: MutableStateFlow<Int>
    private lateinit var sut: ArrivalTextComponent

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        textView = spyk(AppCompatTextView(context))
        textAppearance = MutableStateFlow(R.style.TextAppearance_AppCompat_Large)
        sut = ArrivalTextComponent(textView, textAppearance)
    }

    @Test
    fun `onAttached should apply textAppearance style to the textView`() {
        sut.onAttached(mockk())

        verify { textView.setTextAppearance(textAppearance.value) }
    }

    @Test
    fun `onAttached should observe apply textAppearance style to the textView`() = runBlocking {
        sut.onAttached(mockk())

        textAppearance.value = R.style.TextAppearance_AppCompat_Small

        verify { textView.setTextAppearance(R.style.TextAppearance_AppCompat_Small) }
    }

    @Test
    fun `onAttached not crash when wrong style resource id is used`() {
        textAppearance.value = Int.MAX_VALUE
        sut.onAttached(mockk())
    }
}

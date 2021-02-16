package com.mapbox.navigation.ui.tripprogress.example

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Toast
import com.mapbox.navigation.ui.base.example.Disabled
import com.mapbox.navigation.ui.base.example.Enabled
import com.mapbox.navigation.ui.base.example.Expected
import com.mapbox.navigation.ui.base.example.Failure
import com.mapbox.navigation.ui.base.example.Success

class ExampleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private lateinit var viewOneMain: View
    private lateinit var viewOneAdditional: View

    private lateinit var viewTwoMain: View

    fun render(expected: Expected<ExampleValue, ExampleError>) {
        when (expected) {
            is Success -> {
                viewOneMain.visibility = VISIBLE
                viewOneMain.update {
                    expected.value.mainFeature
                }

                when (val feature = expected.value.additionalFeature) {
                    is Enabled -> {
                        viewOneAdditional.visibility = VISIBLE
                        viewOneAdditional.update {
                            feature.value
                        }
                    }
                    Disabled -> {
                        viewOneAdditional.visibility = GONE
                    }
                }
            }
            is Failure -> {
                viewOneMain.visibility = GONE
                viewOneAdditional.visibility = GONE
                Toast.makeText(context, expected.error.errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun renderAnother(expected: Expected<AnotherExampleValue, AnotherExampleError>) {
        when (expected) {
            is Success -> {
                viewTwoMain.visibility = VISIBLE
                viewTwoMain.update {
                    expected.value.mainFeature
                }
            }
            is Failure -> {
                viewTwoMain.visibility = GONE
                Toast.makeText(context, expected.error.errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
}

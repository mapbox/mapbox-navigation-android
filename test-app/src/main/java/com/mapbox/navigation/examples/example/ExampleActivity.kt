package com.mapbox.navigation.examples.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.mapbox.navigation.ui.base.example.Disabled
import com.mapbox.navigation.ui.base.example.Failure
import com.mapbox.navigation.ui.base.example.FeatureState
import com.mapbox.navigation.ui.base.example.Success
import com.mapbox.navigation.ui.tripprogress.example.AnotherExampleError
import com.mapbox.navigation.ui.tripprogress.example.ExampleApi
import com.mapbox.navigation.ui.tripprogress.example.ExampleApiOptions
import com.mapbox.navigation.ui.tripprogress.example.ExampleView

class ExampleActivity : AppCompatActivity() {

    private val exampleApi = ExampleApi(
        ExampleApiOptions.Builder()
            .build()
    )

    private lateinit var exampleView: ExampleView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exampleView = ExampleView(this)
        mainLayout.addView(exampleView)

        // rendering initial state
        exampleView.render(exampleApi.getUpdate())
        exampleView.visibility = View.VISIBLE
    }

    fun onSomeDataUpdated(data: Any) {
        // rendering main feature based on a data
        exampleView.render(
            exampleApi.getUpdate(data).also {
                // example use-case - if our main feature fails, we don't want to show anything
                when (it) {
                    is Success -> {
                        it.value.mutate {
                            it.additionalFeature = Disabled
                        }
                        exampleView.visibility = View.VISIBLE
                    }
                    is Failure -> exampleView.visibility = View.GONE
                }
            }
        )

        // rendering sub feature based on a data
        exampleView.renderAnother(exampleApi.getAnotherUpdate(data))
    }
}

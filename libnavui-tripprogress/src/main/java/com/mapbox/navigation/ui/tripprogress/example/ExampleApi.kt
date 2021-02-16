package com.mapbox.navigation.ui.tripprogress.example

import com.mapbox.navigation.ui.base.example.Disabled
import com.mapbox.navigation.ui.base.example.Enabled
import com.mapbox.navigation.ui.base.example.Expected
import com.mapbox.navigation.ui.base.example.Failure
import com.mapbox.navigation.ui.base.example.Success

class ExampleApi(private val options: ExampleApiOptions) {
    fun getUpdate(data: Any? = null): Expected<ExampleValue, ExampleError> {
        val successCondition = true
        return if (successCondition) {
            val mainValue = 1
            val additionalFeature = if (options.additionalFeatureEnabled) {
                Enabled(1.1)
            } else {
                Disabled
            }
            Success(ExampleValue(mainValue, additionalFeature))
        } else {
            Failure(ExampleError("something went wrong"))
        }
    }

    fun getAnotherUpdate(data: Any): Expected<AnotherExampleValue, AnotherExampleError> {
        val successCondition = true
        return if (successCondition) {
            val mainValue = "some result"
            Success(AnotherExampleValue(mainValue))
        } else {
            Failure(AnotherExampleError("something went wrong"))
        }
    }
}

class ExampleApiOptions private constructor(
    val additionalFeatureEnabled: Boolean
) {
    class Builder {
        private var additionalFeatureEnabled = false

        fun additionalFeatureEnabled(additionalFeatureEnabled: Boolean) =
            apply { this.additionalFeatureEnabled = additionalFeatureEnabled }

        fun build() = ExampleApiOptions(additionalFeatureEnabled)
    }
}

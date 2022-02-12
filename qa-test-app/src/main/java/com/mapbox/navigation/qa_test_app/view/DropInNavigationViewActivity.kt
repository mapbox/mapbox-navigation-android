package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.Rounding
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.options.DeviceProfile
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.qa_test_app.databinding.DropinNavigationViewActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.lifecycle.DropInNavigationView
import com.mapbox.navigation.qa_test_app.utils.Utils

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DropInNavigationViewActivity : AppCompatActivity() {

    private val binding: DropinNavigationViewActivityLayoutBinding by lazy {
        DropinNavigationViewActivityLayoutBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * Customize [NavigationOptions] before constructing the [DropInNavigationView].
         * You can do this be by setting up [MapboxNavigationApp] before setContentView.
         */
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this)
                .accessToken(Utils.getMapboxAccessToken(this))
                .deviceProfile(
                    DeviceProfile.Builder()
                        .deviceType(DeviceType.AUTOMOBILE)
                        .build()
                )
                .distanceFormatterOptions(
                    DistanceFormatterOptions.Builder(this)
                        .unitType(UnitType.METRIC)
                        .roundingIncrement(Rounding.INCREMENT_TWENTY_FIVE)
                        .build()
                )
                .build()
        )

        setContentView(binding.root)
    }
}

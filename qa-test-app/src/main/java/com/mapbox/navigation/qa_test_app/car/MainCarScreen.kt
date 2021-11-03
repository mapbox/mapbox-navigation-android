package com.mapbox.navigation.qa_test_app.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.navigation.qa_test_app.R

class MainCarScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        return NavigationTemplate.Builder()
            .setBackgroundColor(CarColor.PRIMARY)
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(buildSettingsAction())
                    .build()
            )
            .build()
    }

    private fun buildSettingsAction() = Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    carContext, R.drawable.ic_settings
                )
            ).build()
        )
        .build()
}

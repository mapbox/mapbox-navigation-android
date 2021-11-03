package com.mapbox.navigation.qa_test_app.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI

@OptIn(ExperimentalMapboxNavigationAPI::class)
class MainCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return MainCarSession()
    }
}

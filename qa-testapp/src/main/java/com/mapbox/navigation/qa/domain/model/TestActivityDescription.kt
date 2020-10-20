package com.mapbox.navigation.qa.domain.model

import com.mapbox.navigation.qa.domain.LaunchActivityFun

data class TestActivityDescription(
    val label: String,
    val fullDescriptionResource: Int,
    val launchActivityFun: LaunchActivityFun
)

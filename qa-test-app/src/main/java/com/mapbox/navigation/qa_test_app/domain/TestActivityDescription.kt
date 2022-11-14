package com.mapbox.navigation.qa_test_app.domain

data class TestActivityDescription(
    val label: String,
    val fullDescriptionResource: Int,
    val category: String = "none",
    /**
     *  When true, request permissions before launching the activity.
     *  When false, launch the activity without requesting permissions.
     *  */
    val launchAfterPermissionResult: Boolean = true,
    val launchActivityFun: LaunchActivityFun,
)

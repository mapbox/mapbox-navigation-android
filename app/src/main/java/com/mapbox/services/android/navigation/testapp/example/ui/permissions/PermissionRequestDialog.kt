package com.mapbox.services.android.navigation.testapp.example.ui.permissions

import android.app.Activity
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import com.mapbox.android.core.permissions.PermissionsManager

class PermissionRequestDialog(private val activity: Activity,
                              private val permissionsManager: PermissionsManager) : AlertDialog(activity) {

  init {
    // TODO string localization
    setTitle("Location Permissions")
    setMessage("To use navigation, you need to grant location permissions.")
    setButton(DialogInterface.BUTTON_POSITIVE, "Request Permissions") { _, _ ->
      permissionsManager.requestLocationPermissions(activity)
    }
    setButton(DialogInterface.BUTTON_NEGATIVE, "Close Navigation") { dialog, _ ->
      dialog.dismiss()
      activity.finish()
    }
  }
}

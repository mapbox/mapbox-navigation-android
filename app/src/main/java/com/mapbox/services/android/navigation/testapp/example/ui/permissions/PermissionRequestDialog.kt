package com.mapbox.services.android.navigation.testapp.example.ui.permissions

import android.app.Activity
import android.content.DialogInterface
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog

const val NAVIGATION_PERMISSIONS_REQUEST = 12345

class PermissionRequestDialog(private val activity: Activity) : AlertDialog(activity) {

  init {
    setTitle("Navigation Permissions")
    setMessage("To use navigation, you need to grant location permissions." +
        "To use offline navigation, you have to enable file storage permissions.")
    setButton(DialogInterface.BUTTON_POSITIVE, "Request Permissions") { _, _ ->

      val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
          android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
      ActivityCompat.requestPermissions(activity, permissions, NAVIGATION_PERMISSIONS_REQUEST)
    }
    setButton(DialogInterface.BUTTON_NEGATIVE, "Close Navigation") { dialog, _ ->
      dialog.dismiss()
      activity.finish()
    }
  }
}

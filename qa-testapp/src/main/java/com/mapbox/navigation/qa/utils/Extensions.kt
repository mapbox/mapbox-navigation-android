package com.mapbox.navigation.qa.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

inline fun <reified T : Activity> Activity.startActivity() {
    startActivity(Intent(this, T::class.java))
}

inline fun <reified T : Activity> Activity.startActivity(flags: Int) {
    val intentToLaunch = Intent(this, T::class.java)
    intentToLaunch.addFlags(flags)
    startActivity(intentToLaunch)
}

inline fun <reified T : Activity> Activity.startActivity(bundle: Bundle?) {
    val intentToLaunch = Intent(this, T::class.java)
    if(bundle != null) {
        intentToLaunch.putExtras(bundle)
    }
    startActivity(intentToLaunch)
}

inline fun <reified T : Activity> Activity.startActivityForResult(requestCode: Int) {
    val intentToLaunch = Intent(this, T::class.java)
    startActivityForResult(intentToLaunch, requestCode)
}


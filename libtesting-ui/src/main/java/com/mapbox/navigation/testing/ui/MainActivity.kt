package com.mapbox.navigation.testing.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var notificationManager: AppNotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_set_text.setOnClickListener {
            et_message.setText(R.string.new_message)
            btn_show_notification.isEnabled = true
        }

        btn_show_notification.setOnClickListener {
            notificationManager.showTestNotification()
        }

        notificationManager = AppNotificationManager(this)
    }
}

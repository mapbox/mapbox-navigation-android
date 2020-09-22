package com.mapbox.navigation.examples.utils.extensions

import android.view.View

var View.show: Boolean
    get() = this.visibility == View.VISIBLE
    set(value) {
        this.visibility = if (value) View.VISIBLE else View.GONE
    }
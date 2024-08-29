package com.mapbox.navigation.ui.components.maneuver.model

import android.content.res.Resources
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.widget.TextView
import com.mapbox.navigation.ui.utils.internal.extensions.drawableWithHeight
import com.mapbox.navigation.ui.utils.internal.extensions.getAsBitmap

internal object ExitStyleGenerator {

    fun styleAndGetExit(
        exitText: String,
        textView: TextView,
        desiredHeight: Int,
        resources: Resources,
    ): SpannableStringBuilder {
        val exitBuilder = SpannableStringBuilder(exitText)
        val exitBitmap = textView.getAsBitmap()
        val exitDrawable = exitBitmap.drawableWithHeight(desiredHeight, resources)
        exitBuilder.setSpan(
            ImageSpan(exitDrawable),
            0,
            exitText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        return exitBuilder
    }
}

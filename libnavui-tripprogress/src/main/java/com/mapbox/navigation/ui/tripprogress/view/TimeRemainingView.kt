package com.mapbox.navigation.ui.tripprogress.view

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * A view that could be used to render time remaining to reach the destination
 */
internal class TimeRemainingView : AppCompatTextView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    fun renderTimeRemaining(
        timeRemaining: SpannableString,
        bufferType: BufferType
    ) {
        this.setText(timeRemaining, bufferType)
    }
}

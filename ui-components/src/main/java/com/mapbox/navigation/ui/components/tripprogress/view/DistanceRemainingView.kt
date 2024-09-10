package com.mapbox.navigation.ui.components.tripprogress.view

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * A view that could be used to render distance remaining to reach the destination
 */
internal class DistanceRemainingView : AppCompatTextView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    fun renderDistanceRemaining(
        distanceRemaining: SpannableString,
        bufferType: BufferType,
    ) {
        this.setText(distanceRemaining, bufferType)
    }
}

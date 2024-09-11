package com.mapbox.navigation.ui.components.tripprogress.view

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * A view that could be used to render estimated arrival time to reach the destination
 */
internal class EstimatedArrivalTimeView : AppCompatTextView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    fun renderEstimatedArrivalTime(
        estimatedArrivalTime: SpannableString,
        bufferType: BufferType,
    ) {
        this.setText(estimatedArrivalTime, bufferType)
    }
}

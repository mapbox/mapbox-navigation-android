package com.mapbox.navigation.ui.tripprogress.view

import android.content.Context
import android.text.SpannableString
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * A view that could be used to render distance remaining to reach the destination
 */
class DistanceRemainingView : AppCompatTextView {

    /**
     *
     * @param context Context
     * @constructor
     */
    constructor(context: Context) : super(context)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @constructor
     */
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    /**
     *
     * @param context Context
     * @param attrs AttributeSet?
     * @param defStyleAttr Int
     * @constructor
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    /**
     * Invoke the function to render distance remaining to reach the destination.
     *
     * @param distanceRemaining value represented in [SpannableString]
     * @param bufferType
     */
    fun renderDistanceRemaining(
        distanceRemaining: SpannableString,
        bufferType: BufferType
    ) {
        this.setText(distanceRemaining, bufferType)
    }
}

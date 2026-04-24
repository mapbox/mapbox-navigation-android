package com.mapbox.navigation.ui.voicefeedback.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.StyleRes
import androidx.annotation.UiThread
import androidx.core.content.ContextCompat
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.voicefeedback.R
import com.mapbox.navigation.ui.voicefeedback.databinding.MapboxVoiceFeedbackButtonLayoutBinding

@ExperimentalPreviewMapboxNavigationAPI
@UiThread
class MapboxVoiceFeedbackButton : FrameLayout {
    private val binding =
        MapboxVoiceFeedbackButtonLayoutBinding.inflate(LayoutInflater.from(context), this)

    val iconImage: ImageView = binding.iconImage

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
        this(context, attrs, defStyleAttr, R.style.MapboxStyleVoiceFeedbackButton)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        context.theme
            .obtainStyledAttributes(
                attrs,
                R.styleable.MapboxVoiceFeedbackButton,
                defStyleAttr,
                defStyleRes,
            ).apply {
                try {
                    applyAttributes(this)
                } finally {
                    recycle()
                }
            }
    }

    fun updateStyle(
        @StyleRes style: Int,
    ) {
        context.obtainStyledAttributes(style, R.styleable.MapboxVoiceFeedbackButton).apply {
            try {
                applyAttributes(this)
            } finally {
                recycle()
            }
        }
    }

    private fun applyAttributes(typedArray: TypedArray) {
        typedArray
            .getResourceId(
                R.styleable.MapboxVoiceFeedbackButton_voiceFeedbackButtonIcon,
                R.drawable.mapbox_ic_voice_feedback,
            ).also {
                iconImage.setImageResource(it)
            }
        typedArray
            .getColorStateList(
                R.styleable.MapboxVoiceFeedbackButton_voiceFeedbackButtonIconTint,
            )?.also {
                iconImage.imageTintList = it
            }
        background =
            typedArray.getDrawable(R.styleable.MapboxVoiceFeedbackButton_voiceFeedbackButtonBackground)
                ?: ContextCompat.getDrawable(context, R.drawable.mapbox_voice_feedback_bg_button)
    }
}

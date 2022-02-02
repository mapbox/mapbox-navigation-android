package com.mapbox.navigation.qa_test_app.view.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.WidgetIconPreviewBinding

class IconPreview : ConstraintLayout {
    private val binding = WidgetIconPreviewBinding.inflate(LayoutInflater.from(context), this, true)

    val iconImageView: AppCompatImageView get() = binding.iconImage
    val nameTextView: AppCompatTextView get() = binding.nameText
    val descriptionTextView: AppCompatTextView get() = binding.descriptionText
    val dividerView: View get() = binding.divider

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.IconPreview, 0, 0).apply {
            try {
                getDrawable(R.styleable.IconPreview_iconPreviewSrc)?.also {
                    iconImageView.setImageDrawable(it)
                }
                getString(R.styleable.IconPreview_iconPreviewName)?.also {
                    nameTextView.text = it
                }
                getString(R.styleable.IconPreview_iconPreviewDescription)?.also {
                    descriptionTextView.text = it
                }
                dividerView.isVisible = getBoolean(R.styleable.IconPreview_iconPreviewDivider, true)
            } finally {
                recycle()
            }
        }
    }
}

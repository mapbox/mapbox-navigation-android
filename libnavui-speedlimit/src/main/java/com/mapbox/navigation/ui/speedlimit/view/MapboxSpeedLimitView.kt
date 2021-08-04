package com.mapbox.navigation.ui.speedlimit.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.ui.speedlimit.R
import com.mapbox.navigation.ui.speedlimit.api.MapboxSpeedLimitApi
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitError
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitValue
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.launch

/**
 * A view component intended to consume data produced by the [MapboxSpeedLimitApi].
 */
class MapboxSpeedLimitView : AppCompatTextView {

    private var speedLimitBackgroundColor: Int = 0
    private var speedLimitViennaBorderColor: Int = 0
    private var speedLimitMutcdBorderColor: Int = 0
    private var speedLimitSign: SpeedLimitSign? = null

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
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttributes(attrs)
    }

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
    ) : super(context, attrs, defStyleAttr) {
        initAttributes(attrs)
    }

    /**
     * Allows you to change the style of [MapboxSpeedLimitView].
     * @param styleResource Int
     */
    fun updateStyle(@StyleRes styleResource: Int) {
        val typedArray: TypedArray = context.obtainStyledAttributes(
            styleResource,
            R.styleable.MapboxSpeedLimitView
        )
        applyAttributes(typedArray)

        typedArray.recycle()
    }

    /**
     * Updates this view with speed limit related data.
     *
     * @param expected a Expected<UpdateSpeedLimitError, UpdateSpeedLimitValue>
     */
    fun render(expected: Expected<UpdateSpeedLimitError, UpdateSpeedLimitValue>) {
        ThreadController.getMainScopeAndRootJob().scope.launch {
            expected.fold(
                { // error
                    val sign = speedLimitSign
                    if (sign != null) {
                        val speedLimitSpan = when (sign) {
                            SpeedLimitSign.MUTCD -> {
                                getSpeedLimitSpannable(
                                    sign,
                                    context.getString(R.string.max_speed_no_value)
                                )
                            }
                            SpeedLimitSign.VIENNA -> {
                                getSpeedLimitSpannable(sign, "--")
                            }
                        }
                        setText(speedLimitSpan, BufferType.SPANNABLE)
                    }
                },
                { value ->
                    speedLimitSign = value.signFormat
                    updateBackgroundSize(value.signFormat)
                    val drawable = getViewDrawable(value.signFormat)
                    val formatterSpeedLimit =
                        value.speedLimitFormatter.format(value)
                    val speedLimitSpan =
                        getSpeedLimitSpannable(value.signFormat, formatterSpeedLimit)

                    background = drawable
                    setText(speedLimitSpan, BufferType.SPANNABLE)
                }
            )
        }
    }

    private fun initAttributes(attrs: AttributeSet?) {
        gravity = Gravity.CENTER
        textSize = context.resources.getDimension(R.dimen.mapbox_dimen_text_6sp)
        val typedArray: TypedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxSpeedLimitView,
            0,
            R.style.MapboxStyleSpeedLimit
        )
        applyAttributes(typedArray)

        typedArray.recycle()
    }

    private fun applyAttributes(typedArray: TypedArray) {
        typedArray.getColor(
            R.styleable.MapboxSpeedLimitView_speedLimitTextColor,
            ContextCompat.getColor(
                context,
                R.color.mapbox_speed_limit_text_color
            )
        ).let {
            setTextColor(it)
        }

        speedLimitBackgroundColor = typedArray.getColor(
            R.styleable.MapboxSpeedLimitView_speedLimitBackgroundColor,
            ContextCompat.getColor(
                context,
                R.color.mapbox_speed_limit_view_background
            )
        )

        speedLimitViennaBorderColor = typedArray.getColor(
            R.styleable.MapboxSpeedLimitView_speedLimitViennaBorderColor,
            ContextCompat.getColor(
                context,
                R.color.mapbox_speed_limit_view_vienna_border
            )
        )

        speedLimitMutcdBorderColor = typedArray.getColor(
            R.styleable.MapboxSpeedLimitView_speedLimitMutcdBorderColor,
            ContextCompat.getColor(
                context,
                R.color.mapbox_speed_limit_view_mutcd_border
            )
        )
    }

    internal fun getViewDrawable(signFormat: SpeedLimitSign): LayerDrawable {
        val drawableShape = getDrawableShape(signFormat)
        val outerBackgroundDrawable = backgroundDrawable(drawableShape, speedLimitBackgroundColor)
        val borderDrawable = borderDrawable(
            drawableShape,
            speedLimitMutcdBorderColor,
            speedLimitViennaBorderColor
        )
        val innerBackgroundDrawable = backgroundDrawable(drawableShape, speedLimitBackgroundColor)
        return LayerDrawable(
            arrayOf(outerBackgroundDrawable, borderDrawable, innerBackgroundDrawable)
        ).also {
            it.setLayerInset(
                0,
                OUTER_BACKGROUND_INSET,
                OUTER_BACKGROUND_INSET,
                OUTER_BACKGROUND_INSET,
                OUTER_BACKGROUND_INSET
            )
            it.setLayerInset(
                1,
                BORDER_INSET,
                BORDER_INSET,
                BORDER_INSET,
                BORDER_INSET
            )
            when (signFormat) {
                SpeedLimitSign.MUTCD -> {
                    it.setLayerInset(
                        2,
                        INNER_BACKGROUND_MUTCD_INSET,
                        INNER_BACKGROUND_MUTCD_INSET,
                        INNER_BACKGROUND_MUTCD_INSET,
                        INNER_BACKGROUND_MUTCD_INSET
                    )
                }
                SpeedLimitSign.VIENNA -> {
                    it.setLayerInset(
                        2,
                        INNER_BACKGROUND_VIENNA_INSET,
                        INNER_BACKGROUND_VIENNA_INSET,
                        INNER_BACKGROUND_VIENNA_INSET,
                        INNER_BACKGROUND_VIENNA_INSET
                    )
                }
            }
        }
    }

    private fun backgroundDrawable(shape: Int, speedLimitBackgroundColor: Int): GradientDrawable {
        val background = GradientDrawable()
        background.setColor(speedLimitBackgroundColor)
        background.shape = shape
        if (shape == GradientDrawable.RECTANGLE) {
            background.cornerRadius = RADIUS
        }
        return background
    }

    private fun borderDrawable(
        shape: Int,
        speedLimitMutcdBorderColor: Int,
        speedLimitViennaBorderColor: Int
    ): GradientDrawable {
        val border = GradientDrawable()
        border.shape = shape
        if (shape == GradientDrawable.RECTANGLE) {
            border.cornerRadius = RADIUS
            border.setColor(speedLimitMutcdBorderColor)
        } else {
            border.setColor(speedLimitViennaBorderColor)
        }
        return border
    }

    private fun getSpeedLimitSpannable(
        sign: SpeedLimitSign,
        value: String
    ): SpannableStringBuilder {
        val sizeSpanStartIndex = getSizeSpanStartIndex(sign, value)

        return SpannableStringBuilder(value).also {
            it.setSpan(
                StyleSpan(Typeface.BOLD),
                sizeSpanStartIndex,
                value.length,
                SPAN_EXCLUSIVE_EXCLUSIVE
            )
            it.setSpan(
                RelativeSizeSpan(2.1f),
                sizeSpanStartIndex,
                value.length,
                SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    internal fun getSizeSpanStartIndex(signFormat: SpeedLimitSign, formattedString: String): Int {
        return when (signFormat) {
            SpeedLimitSign.MUTCD -> formattedString.indexOf("MAX\n") + "MAX\n".length
            SpeedLimitSign.VIENNA -> formattedString.indexOf("\n") + 1
        }
    }

    private fun getDrawableShape(signFormat: SpeedLimitSign): Int {
        return when (signFormat) {
            SpeedLimitSign.MUTCD -> GradientDrawable.RECTANGLE
            SpeedLimitSign.VIENNA -> GradientDrawable.OVAL
        }
    }

    private fun updateBackgroundSize(signFormat: SpeedLimitSign) {
        val density = context.resources.displayMetrics.density
        when (signFormat) {
            SpeedLimitSign.MUTCD -> {
                width = (60 * density).toInt()
                height = (70 * density).toInt()
            }
            SpeedLimitSign.VIENNA -> {
                width = (65 * density).toInt()
                height = (65 * density).toInt()
            }
        }
    }

    private companion object {
        private const val RADIUS = 10f
        private const val OUTER_BACKGROUND_INSET = 3
        private const val BORDER_INSET = 6
        private const val INNER_BACKGROUND_MUTCD_INSET = 9
        private const val INNER_BACKGROUND_VIENNA_INSET = 14
    }
}

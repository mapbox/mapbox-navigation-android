package com.mapbox.navigation.ui.maneuver.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.ui.base.model.maneuver.ComponentNode
import com.mapbox.navigation.ui.base.model.maneuver.ExitNumberComponentNode
import com.mapbox.navigation.ui.maneuver.R

/**
 * Default Exit View that renders exit number in a specific style.
 * @property leftDrawable Drawable? denotes the style for exit sign that is on the left.
 * @property rightDrawable Drawable? denotes the style for exit sign that is on the right.
 * @property exitBackground Drawable? denotes the exit board style.
 */
class MapboxExitText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var leftDrawable = ContextCompat.getDrawable(
        context, R.drawable.mapbox_ic_exit_arrow_left
    )
    private var rightDrawable = ContextCompat.getDrawable(
        context, R.drawable.mapbox_ic_exit_arrow_right
    )
    private var exitBackground = ContextCompat.getDrawable(
        context, R.drawable.mapbox_exit_board_background
    )

    init {
        setTextColor(ContextCompat.getColor(context, R.color.maneuverExitTextColor))
        includeFontPadding = false
        setTypeface(typeface, Typeface.BOLD)
        textSize = 10 * context.resources.displayMetrics.scaledDensity
    }

    /**
     * Invoke the method to change the drawables defining the exit style.
     * @param background Drawable? denotes the exit board style.
     * @param leftDrawable Drawable? denotes the style for exit sign that is on the left.
     * @param rightDrawable Drawable? denotes the style for exit sign that is on the right.
     */
    fun setExitStyle(
        background: Drawable?,
        leftDrawable: Drawable?,
        rightDrawable: Drawable?
    ) {
        exitBackground = background
        this.leftDrawable = leftDrawable
        this.rightDrawable = rightDrawable
    }

    /**
     * Invoke the method to set the exit number to the view.
     * @param modifier String? represents either [ManeuverModifier.LEFT] or [ManeuverModifier.RIGHT].
     * Default value is [ManeuverModifier.LEFT]
     * @param exit ExitNumberComponentNode [ComponentNode] of the type [BannerComponents.EXIT_NUMBER]
     */
    fun setExit(modifier: String?, exit: ExitNumberComponentNode) {
        when (modifier) {
            ManeuverModifier.LEFT -> {
                setCompoundDrawablesWithIntrinsicBounds(
                    leftDrawable,
                    null,
                    null,
                    null
                )
            }
            ManeuverModifier.RIGHT -> {
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    rightDrawable,
                    null
                )
            }
            else -> {
                setCompoundDrawablesWithIntrinsicBounds(
                    leftDrawable,
                    null,
                    null,
                    null
                )
            }
        }
        text = exit.text
        background = exitBackground
    }

    /**
     * Invoke to access the [MapboxExitText] in the form of [Bitmap]
     * @return Bitmap
     */
    fun getViewAsBitmap(): Bitmap {
        val measureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measure(measureSpec, measureSpec)
        layout(0, 0, measuredWidth, measuredHeight)
        val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

    /**
     * Invoke to convert a given [Bitmap] to a [Drawable] of the desired height.
     * @param bitmap Bitmap
     * @param drawableHeight Int
     * @return Drawable
     */
    fun styleExitWith(
        bitmap: Bitmap,
        drawableHeight: Int
    ): Drawable {
        val drawable: Drawable = BitmapDrawable(context.resources, bitmap)
        val right = (drawableHeight * bitmap.width.toDouble() / bitmap.height.toDouble()).toInt()
        drawable.setBounds(0, 0, right, drawableHeight)
        return drawable
    }
}

package com.mapbox.navigation.ui.androidauto.ui.maneuver.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.StyleRes
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.TextViewCompat
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.navigation.tripdata.maneuver.model.ComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.ExitNumberComponentNode
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.ui.maneuver.model.MapboxExitProperties
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.tripdata.R as TripDataR

/**
 * Default Exit View that renders exit number in a specific style.
 * @property leftDrawable Drawable? denotes the style for exit sign that is on the left.
 * @property rightDrawable Drawable? denotes the style for exit sign that is on the right.
 * @property exitBackground Drawable? denotes the exit board style.
 */
@UiThread
class MapboxExitText : AppCompatTextView {

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
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    private var leftDrawable = ContextCompat.getDrawable(
        context,
        TripDataR.drawable.mapbox_ic_exit_arrow_left_mutcd,
    )
    private var rightDrawable = ContextCompat.getDrawable(
        context,
        TripDataR.drawable.mapbox_ic_exit_arrow_right_mutcd,
    )
    private var exitBackground = ContextCompat.getDrawable(
        context,
        TripDataR.drawable.mapbox_exit_board_background,
    )
    private var exitProperties: MapboxExitProperties? = null

    /**
     * Invoke the method to set the properties you want [MapboxExitText] to use to render
     * exit.
     * @param properties MapboxExitProperties
     */
    fun updateExitProperties(
        properties: MapboxExitProperties?,
    ) {
        this.exitProperties = properties
        if (properties != null) {
            this.exitBackground = ContextCompat.getDrawable(context, properties.exitBackground)
        }
    }

    /**
     * Allows you to change the text appearance of [MapboxPrimaryManeuver], [MapboxSecondaryManeuver]
     * and [MapboxSubManeuver].
     * @see [TextViewCompat.setTextAppearance]
     * @param style Int
     */
    fun updateTextAppearance(@StyleRes style: Int) {
        TextViewCompat.setTextAppearance(this, style)
    }

    /**
     * Invoke the method to set the exit number to the view.
     * @param modifier String? the side of the exit. [ManeuverModifier.LEFT],
     * [ManeuverModifier.SLIGHT_LEFT] and [ManeuverModifier.SHARP_LEFT] draw the left exit arrow,
     * and [ManeuverModifier.RIGHT], [ManeuverModifier.SLIGHT_RIGHT] and
     * [ManeuverModifier.SHARP_RIGHT] draw the right one. Any other value uses the fallback.
     * @param exit ExitNumberComponentNode [ComponentNode] of the type [BannerComponents.EXIT_NUMBER]
     */
    fun setExit(modifier: String?, exit: ExitNumberComponentNode) {
        val exitText = when (modifier) {
            ManeuverModifier.LEFT,
            ManeuverModifier.SLIGHT_LEFT,
            ManeuverModifier.SHARP_LEFT,
            -> {
                val drawable = ifNonNull(this.exitProperties?.exitLeftDrawable) { leftDrawable ->
                    ContextCompat.getDrawable(context, leftDrawable).adjustDrawableHeight()
                } ?: leftDrawable.adjustDrawableHeight()
                setCompoundDrawablesWithIntrinsicBounds(
                    drawable,
                    null,
                    null,
                    null,
                )
                exit.text
            }
            ManeuverModifier.RIGHT,
            ManeuverModifier.SLIGHT_RIGHT,
            ManeuverModifier.SHARP_RIGHT,
            -> {
                val drawable = ifNonNull(this.exitProperties?.exitRightDrawable) { rightDrawable ->
                    ContextCompat.getDrawable(context, rightDrawable).adjustDrawableHeight()
                } ?: rightDrawable.adjustDrawableHeight()
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawable,
                    null,
                )
                exit.text
            }
            else -> {
                if (exitProperties == null) {
                    setCompoundDrawablesWithIntrinsicBounds(
                        null,
                        null,
                        rightDrawable,
                        null,
                    )
                    exit.text
                } else {
                    getFallbackExitText(exit, exitProperties!!)
                }
            }
        }
        text = exitText
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
    fun styleExitWith(bitmap: Bitmap, drawableHeight: Int): Drawable {
        val drawable: Drawable = BitmapDrawable(context.resources, bitmap)
        val right = (drawableHeight * bitmap.width.toDouble() / bitmap.height.toDouble()).toInt()
        drawable.setBounds(0, 0, right, drawableHeight)
        return drawable
    }

    private fun getFallbackExitText(
        exit: ExitNumberComponentNode,
        exitProperties: MapboxExitProperties,
    ): String {
        when (exitProperties) {
            is MapboxExitProperties.PropertiesMutcd -> {
                return when {
                    exitProperties.shouldFallbackWithDrawable -> {
                        setCompoundDrawablesWithIntrinsicBounds(
                            null,
                            null,
                            ContextCompat.getDrawable(
                                context,
                                exitProperties.fallbackDrawable,
                            ).adjustDrawableHeight(),
                            null,
                        )
                        exit.text
                    }
                    exitProperties.shouldFallbackWithText -> {
                        context.getString(R.string.car_maneuver_exit_number, exit.text)
                    }
                    else -> {
                        exit.text
                    }
                }
            }
            is MapboxExitProperties.PropertiesVienna -> {
                return when {
                    exitProperties.shouldFallbackWithDrawable -> {
                        setCompoundDrawablesWithIntrinsicBounds(
                            ContextCompat.getDrawable(
                                context,
                                exitProperties.fallbackDrawable,
                            ).adjustDrawableHeight(),
                            null,
                            null,
                            null,
                        )
                        exit.text
                    }
                    exitProperties.shouldFallbackWithText -> {
                        context.getString(R.string.car_maneuver_exit_number, exit.text)
                    }
                    else -> {
                        exit.text
                    }
                }
            }
        }
    }

    private fun Drawable?.adjustDrawableHeight(): Drawable? {
        val bitmap = this?.toBitmap(lineHeight, lineHeight, Bitmap.Config.ARGB_8888)
        return bitmap?.toDrawable(context.resources)
    }
}

package com.mapbox.navigation.ui.shield.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import com.mapbox.navigation.ui.utils.internal.extensions.drawableWithHeight
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import java.io.ByteArrayInputStream


/**
 * View to render the route shields
 */
class MapboxRouteShieldView : AppCompatImageView {

    /*private val binding = MapboxRouteShieldViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )*/

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

    /*fun render(shield: Expected<RouteShieldError, RouteShield>) {
        shield.fold(
            { routeShieldError ->

            },{ routeShield ->
                val spriteWidth = routeShield.sprite?.spriteAttributes()?.width()?.toFloat()
                val spriteHeight = routeShield.sprite?.spriteAttributes()?.height()?.toFloat()
                ifNonNull(spriteWidth, spriteHeight) { widthDp, heightDp ->
                    val widthPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        widthDp,
                        resources.displayMetrics
                    ).toInt()
                    val heightPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        heightDp,
                        resources.displayMetrics
                    ).toInt()
                    val stream = ByteArrayInputStream(routeShield.shield)
                    val bitmap = SvgUtil.renderAsBitmapWith(stream, widthPx, heightPx)
                    val drawable = bitmap?.drawableWithHeight(heightPx, resources)
                    this.setImageDrawable(drawable)
                } ?: return@fold
            }
        )
    }*/
}

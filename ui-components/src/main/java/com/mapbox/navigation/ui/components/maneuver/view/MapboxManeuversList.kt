package com.mapbox.navigation.ui.components.maneuver.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

/**
 * Class extending [RecyclerView] to intercept request layout.
 */
internal class MapboxManeuversList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {

    private var mRequestedLayout = false

    /**
     * We need to intercept this method because if we don't our children will never update
     * Check https://stackoverflow.com/questions/49371866/recyclerview-wont-update-child-until-i-scroll
     */
    @SuppressLint("WrongCall")
    @Override
    override fun requestLayout() {
        super.requestLayout()
        if (!mRequestedLayout) {
            mRequestedLayout = true
            post {
                mRequestedLayout = false
                layout(left, top, right, bottom)
                onLayout(false, left, top, right, bottom)
            }
        }
    }
}

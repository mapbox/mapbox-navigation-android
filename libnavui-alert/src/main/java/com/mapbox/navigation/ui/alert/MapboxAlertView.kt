package com.mapbox.navigation.ui.alert

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.mapbox.navigation.ui.base.View
import com.mapbox.navigation.ui.base.model.AlertState

/**
 * It draws a custom view that renders [AlertState]
 */
class MapboxAlertView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), View<AlertState> {

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_alert_view, this, true)
        /*attrs?.let { attributeSet ->
            context.getStyledAttributes(attributeSet, R.styleable.MapboxNavAlertView) {
                alertTextView.setTextColor(
                    ContextCompat.getColor(
                        context, getResourceId(R.styleable.MapboxNavAlertView_avTextColor, Color.BLUE)
                    )
                )
            }
        }*/
    }

    /**
     * Entry point for [MapboxAlertView]
     * @param state AlertState state to render on the screen
     */
    override fun render(state: AlertState) {
    }
}

package com.mapbox.navigation.ui.maps.recenterbutton.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.OvershootInterpolator
import android.view.animation.TranslateAnimation
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.recenterbutton.RecenterButtonState
import com.mapbox.navigation.ui.maps.R

/**
 * Button used to re-activate following user location during navigation.
 *
 * If a user scrolls the map while navigating, the
 * {@link com.mapbox.navigation.ui.summary.SummaryBottomSheet}
 * is set to hidden and this button is shown.
 *
 * This button uses a custom [TranslateAnimation] with [OvershootInterpolator]
 * to be shown.
 */

class MapboxRecenterButton : ConstraintLayout, MapboxView<RecenterButtonState> {

    private var multiOnClickListener: MultiOnClickListener? = MultiOnClickListener()
    private var slideUpBottom: Animation? = null
    private var recenterFab: FloatingActionButton? = null
    private var primaryColor = 0
    private var secondaryColor = 0

    constructor(context: Context) : super(context) {
        initialize(context)
    }
    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs){
        initialize(context)
        initAttributes(attrs)
    }
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int = -1
    ) : super(context, attrs, defStyleAttr) {
        initialize(context)
        initAttributes(attrs)
    }

    /**
     * Sets visibility to VISIBLE and starts custom animation.
     */
    fun show() {
        if (visibility == INVISIBLE) {
            visibility = VISIBLE
            startAnimation(slideUpBottom)
        }
    }

    /**
     * Adds an onClickListener to the button
     *
     * @param onClickListener to add
     */
    fun addOnClickListener(onClickListener: OnClickListener) {
        multiOnClickListener?.addListener(onClickListener)
    }

    /**
     * Removes an onClickListener from the button
     *
     * @param onClickListener to remove
     */
    fun removeOnClickListener(onClickListener: OnClickListener) {
        multiOnClickListener?.removeListener(onClickListener)
    }

    /**
     * Sets visibility to INVISIBLE.
     */
    fun hide() {
        if (visibility == VISIBLE) {
            visibility = INVISIBLE
        }
    }

    /**
     * Use it to update the view style
     *
     * @param styleRes style resource
     */
    fun updateStyle(styleRes: Int) {
        val typedArray = context.obtainStyledAttributes(
            styleRes,
            R.styleable.MapboxStyleRecenterButton
        )
        primaryColor = ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxStyleRecenterButton_recenterButtonPrimaryColor,
                R.color.mapbox_recenter_button_primary
            )
        )
        secondaryColor = ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxStyleRecenterButton_recenterButtonSecondaryColor,
                R.color.mapbox_recenter_button_secondary
            )
        )
        typedArray.recycle()
        applyAttributes()
    }

    /**
     * Once inflation of the view has finished,
     * create the custom animation.
     */
    override fun onFinishInflate() {
        super.onFinishInflate()
        bind()
        applyAttributes()
        initAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupOnClickListeners()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearListeners()
    }

    private fun setupOnClickListeners() {
        recenterFab?.setOnClickListener(multiOnClickListener)
    }

    private fun clearListeners() {
        multiOnClickListener?.clearListeners()
        multiOnClickListener = null
        recenterFab?.setOnClickListener(null)
    }

    /**
     * Inflates the layout.
     */
    private fun initialize(context: Context) {
        inflate(context, R.layout.mapbox_button_recenter, this)
    }

    private fun bind() {
        recenterFab = findViewById(R.id.recenterFab)
    }

    private fun applyAttributes() {
        recenterFab?.backgroundTintList = ColorStateList.valueOf(primaryColor)
        recenterFab?.setColorFilter(secondaryColor)
    }

    private fun initAttributes(attributeSet: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.MapboxStyleRecenterButton
        )
        primaryColor = ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxStyleRecenterButton_recenterButtonPrimaryColor,
                R.color.mapbox_recenter_button_primary
            )
        )
        secondaryColor = ContextCompat.getColor(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxStyleRecenterButton_recenterButtonSecondaryColor,
                R.color.mapbox_recenter_button_secondary
            )
        )
        typedArray.recycle()
    }

    /**
     * Creates the custom animation used to show this button.
     */
    private fun initAnimation() {
        slideUpBottom = TranslateAnimation(
            0f,
            0f,
            125f,
            0f
        ).also {
            slideUpBottom?.duration = 300
            slideUpBottom?.interpolator = OvershootInterpolator(2.0f)
        }
    }

    override fun render(state: RecenterButtonState) {
        when (state) {
            is RecenterButtonState.RecenterButtonVisible -> {
                if (state.isVisible) {
                    show()
                } else {
                    hide()
                }
            }
            is RecenterButtonState.RecenterButtonClicked -> {
//                TODO: ???
            }
        }
    }
}
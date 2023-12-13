package com.mapbox.navigation.ui.status.view

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.AnimatorRes
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.mapbox.navigation.ui.status.R
import com.mapbox.navigation.ui.status.databinding.MapboxStatusViewLayoutBinding
import com.mapbox.navigation.ui.status.internal.extensions.doOnFinish
import com.mapbox.navigation.ui.status.model.Status
import com.mapbox.navigation.utils.internal.isInvisible
import com.mapbox.navigation.utils.internal.isVisible

/**
 * View for rendering [Status] information.
 */
@Suppress("MemberVisibilityCanBePrivate")
@UiThread
class MapboxStatusView : FrameLayout {

    private val binding = MapboxStatusViewLayoutBinding.inflate(LayoutInflater.from(context), this)
    private var pendingHideAnimation: Animator? = null

    /**
     * Currently rendered [Status].
     */
    var currentStatus: Status? = null
        private set

    /**
     * Container view that hosts [spinnerProgressBar], [iconImage] and [messageTextView].
     */
    val containerView: ConstraintLayout = binding.container

    /**
     * Indeterminate Progress indicator displayed when [Status.spinner] is `true`.
     */
    val spinnerProgressBar: ProgressBar = binding.progressBar

    /**
     * Icon Image displayed when [Status.icon] is set.
     */
    val iconImage: AppCompatImageView = binding.iconImage

    /**
     * TextView used to display [Status.message].
     */
    val messageTextView: AppCompatTextView = binding.messageText

    /**
     * Returns `true` if this view is visible.
     */
    val isRendered: Boolean get() = isVisible

    /**
     * A resource identifier for the Show Animator.
     */
    @AnimatorRes
    var showAnimRes: Int = 0

    /**
     * A resource identifier for the Hide Animator.
     */
    @AnimatorRes
    var hideAnimRes: Int = 0

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : this(context, attrs, defStyleAttr, R.style.MapboxStyleStatusView)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        if (!isInEditMode) {
            isInvisible = true
        }

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxStatusView,
            0,
            R.style.MapboxStyleStatusView
        ).apply {
            try {
                showAnimRes = getResourceId(
                    R.styleable.MapboxStatusView_statusViewShowAnimator,
                    android.R.animator.fade_in
                )
                hideAnimRes = getResourceId(
                    R.styleable.MapboxStatusView_statusViewHideAnimator,
                    android.R.animator.fade_out
                )

                // Spinner ProgressBar attributes
                getDrawable(R.styleable.MapboxStatusView_statusViewProgressBarDrawable)?.also {
                    spinnerProgressBar.indeterminateDrawable = it
                }
                getColorStateList(R.styleable.MapboxStatusView_statusViewProgressBarTint)?.also {
                    spinnerProgressBar.indeterminateTintList = it
                }

                // Icon attributes
                getColorStateList(R.styleable.MapboxStatusView_statusViewIconTint)?.also {
                    iconImage.imageTintList = it
                }

                // Message TextView attributes
                getResourceId(
                    R.styleable.MapboxStatusView_statusViewTextAppearance,
                    R.style.MapboxStyleStatusView_TextAppearance
                ).also {
                    // setTextAppearance is not deprecated in AppCompatTextView
                    messageTextView.setTextAppearance(context, it)
                }
            } finally {
                recycle()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        currentStatus = null
        cancelPendingAnimations()
    }

    /**
     * Update this view to render [Status].
     * If other status is already visible, it will hide it first before presenting new one.
     * Show/hide animation is controlled by [Status.animated] flag of presented status.
     * Passing `null` does nothing.
     *
     * This method must be called from the Main Thread.
     *
     * @param status [Status]
     */
    fun render(status: Status?) {
        if (status == null) return

        currentStatus = status
        show(status.animated)
    }

    /**
     * Hide this view if rendered.
     * This method does nothing if the view is not rendered.
     *
     * This method must be called from the Main Thread.
     *
     * @param animated Boolean animate hiding of the view.
     * Passing `null` will use [currentStatus].animated flag value.
     */
    fun cancel(animated: Boolean? = null) {
        if (!isRendered) return
        hide(animated ?: currentStatus?.animated ?: true)
    }

    private fun show(animated: Boolean) = currentStatus?.also { status ->
        cancelPendingAnimations()

        val animations = mutableListOf<Animator>().apply {
            if (isRendered) add(hideAnimator())
            add(showAnimator(status))
            if (status.duration > 0 && status.duration < Long.MAX_VALUE) {
                val delayedHideAnim = hideAnimator(delay = status.duration)
                pendingHideAnimation = delayedHideAnim
                add(delayedHideAnim)
            }
        }

        AnimatorSet().apply {
            playSequentially(animations)
            if (!animated) duration = 0
            start()
        }
    }

    private fun hide(animated: Boolean) {
        cancelPendingAnimations()
        hideAnimator().apply {
            if (!animated) duration = 0
            start()
        }
    }

    private fun updateView(status: Status) {
        messageTextView.text = status.message
        spinnerProgressBar.isVisible = status.spinner
        iconImage.isVisible = 0 < status.icon
        iconImage.setImageResource(status.icon)
    }

    private fun showAnimator(status: Status): Animator =
        AnimatorInflater.loadAnimator(context, showAnimRes).apply {
            setTarget(this@MapboxStatusView)
            addListener(
                object : AnimatorListener {
                    override fun onAnimationStart(p0: Animator) {
                        isInvisible = false
                        updateView(status)
                    }

                    override fun onAnimationEnd(p0: Animator) = Unit

                    override fun onAnimationCancel(p0: Animator) = Unit

                    override fun onAnimationRepeat(p0: Animator) = Unit
                }
            )
        }

    private fun hideAnimator(delay: Long = 0): Animator =
        AnimatorInflater.loadAnimator(context, hideAnimRes).apply {
            setTarget(this@MapboxStatusView)
            startDelay = delay
            doOnFinish {
                isInvisible = true
            }
        }

    private fun cancelPendingAnimations() {
        pendingHideAnimation?.cancel()
    }
}

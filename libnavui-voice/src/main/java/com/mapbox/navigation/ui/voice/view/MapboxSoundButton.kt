package com.mapbox.navigation.ui.voice.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.utils.internal.extensions.afterMeasured
import com.mapbox.navigation.ui.utils.internal.extensions.measureTextWidth
import com.mapbox.navigation.ui.utils.internal.extensions.play
import com.mapbox.navigation.ui.utils.internal.extensions.slideWidth
import com.mapbox.navigation.ui.voice.R
import com.mapbox.navigation.ui.voice.databinding.MapboxSoundButtonLayoutBinding

/**
 * Default view to allow user to mute or unmute voice instructions.
 */
class MapboxSoundButton : ConstraintLayout {

    private var shrunkWidth = 0
    private var muteDrawable: Drawable? = null
    private var unmuteDrawable: Drawable? = null
    private val binding = MapboxSoundButtonLayoutBinding.inflate(
        LayoutInflater.from(context),
        this
    )
    private val mainHandler = Handler(Looper.getMainLooper())

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

    private fun initAttributes(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.MapboxSoundButton
        )
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    private fun applyAttributes(typedArray: TypedArray) {
        muteDrawable = ContextCompat.getDrawable(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxSoundButton_soundButtonMuteDrawable,
                R.drawable.mapbox_ic_sound_off
            )
        )
        unmuteDrawable = ContextCompat.getDrawable(
            context,
            typedArray.getResourceId(
                R.styleable.MapboxSoundButton_soundButtonUnmuteDrawable,
                R.drawable.mapbox_ic_sound_on
            )
        )

        val background = typedArray.getDrawable(R.styleable.MapboxSoundButton_soundButtonBackground)
        if (background != null) {
            binding.soundButtonIcon.background = background
            binding.soundButtonText.background = background
        }

        typedArray.getColorStateList(R.styleable.MapboxSoundButton_soundButtonTextColor)
            ?.let { binding.soundButtonText.setTextColor(it) }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding.soundButtonText.afterMeasured {
            shrunkWidth = width
        }
    }

    /**
     * Allows you to change the style of [MapboxSoundButton].
     * @param style Int
     */
    fun updateStyle(@StyleRes style: Int) {
        val typedArray = context.obtainStyledAttributes(
            style,
            R.styleable.MapboxSoundButton
        )
        applyAttributes(typedArray)
        typedArray.recycle()
    }

    /**
     * Invoke the function to mute.
     * @return `true` representing that view is in muted state.
     */
    fun mute(): Boolean {
        binding.soundButtonIcon.setImageDrawable(muteDrawable)
        return true
    }

    /**
     * Invoke the function to unmute.
     * @return `false` representing that view is in unmuted state.
     */
    fun unmute(): Boolean {
        binding.soundButtonIcon.setImageDrawable(unmuteDrawable)
        return false
    }

    /**
     * Invoke the function to mute and show optional text associated with the action.
     * @param duration for the view to be in the extended mode before it starts to shrink.
     * @param text for the view to show in the extended mode.
     * @return `true` representing that view is in muted state.
     */
    @JvmOverloads
    fun muteAndExtend(
        duration: Long,
        text: String = context.getString(R.string.mapbox_muted),
    ): Boolean {
        return mute().also { showTextWithAnimation(text, duration) }
    }

    /**
     * Invoke the function to unmute and show optional text associated with the action.
     * @param duration for the view to be in the extended mode before it starts to shrink.
     * @param text for the view to show in the extended mode.
     * @return `false` representing that view is in unmuted state.
     */
    @JvmOverloads
    fun unmuteAndExtend(
        duration: Long,
        text: String = context.getString(R.string.mapbox_unmuted),
    ): Boolean {
        return unmute().also { showTextWithAnimation(text, duration) }
    }

    private fun showTextWithAnimation(text: String, duration: Long) {
        val extendedWidth = (binding.soundButtonText.measureTextWidth(text) + shrunkWidth)
            .coerceAtLeast(MIN_EXTENDED_WIDTH * context.resources.displayMetrics.density)
        mainHandler.removeCallbacksAndMessages(null)
        getAnimator(shrunkWidth, extendedWidth.toInt()).play(
            doOnStart = {
                binding.soundButtonText.text = text
                binding.soundButtonText.visibility = View.VISIBLE
                mainHandler.postDelayed(
                    {
                        getAnimator(extendedWidth.toInt(), shrunkWidth).play(
                            doOnStart = {
                                binding.soundButtonText.text = null
                            },
                            doOnEnd = {
                                binding.soundButtonText.visibility = View.INVISIBLE
                            }
                        )
                    },
                    duration
                )
            }
        )
    }

    private fun getAnimator(from: Int, to: Int) =
        binding.soundButtonText.slideWidth(from, to, SLIDE_DURATION)

    private companion object {
        private const val SLIDE_DURATION = 300L
        private const val MIN_EXTENDED_WIDTH = 165
    }
}

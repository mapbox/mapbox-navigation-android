package com.mapbox.navigation.ui.voice.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.utils.internal.extensions.afterMeasured
import com.mapbox.navigation.ui.utils.internal.extensions.extend
import com.mapbox.navigation.ui.utils.internal.extensions.shrink
import com.mapbox.navigation.ui.utils.internal.extensions.slideWidth
import com.mapbox.navigation.ui.voice.R
import com.mapbox.navigation.ui.voice.databinding.MapboxSoundButtonLayoutBinding

/**
 * Default view to allow user to mute or unmute voice instructions.
 */
class MapboxSoundButton : ConstraintLayout {

    private var textWidth = 0
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
            textWidth = width
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
     * @param callback MapboxNavigationConsumer<Boolean> invoked after the drawable has been set and
     * returns true representing that view is in muted state.
     */
    @JvmOverloads
    fun mute(callback: MapboxNavigationConsumer<Boolean>? = null) {
        binding.soundButtonIcon.setImageDrawable(muteDrawable)
        callback?.accept(true)
    }

    /**
     * Invoke the function to unmute.
     * @param callback MapboxNavigationConsumer<Boolean> invoked after the drawable has been set and
     * returns false representing that view is in unmuted state.
     */
    @JvmOverloads
    fun unmute(callback: MapboxNavigationConsumer<Boolean>? = null) {
        binding.soundButtonIcon.setImageDrawable(unmuteDrawable)
        callback?.accept(false)
    }

    /**
     * Invoke the function to mute and show optional text associated with the action.
     * @param duration for the view to be in the extended mode before it starts to shrink.
     * @param callback MapboxNavigationConsumer<Boolean> invoked after the animation is finished and
     * returns true representing that view is in muted state.
     */
    @JvmOverloads
    fun muteAndExtend(duration: Long, callback: MapboxNavigationConsumer<Boolean>? = null) {
        showTextWithAnimation(R.string.mapbox_muted, duration) {
            mute(callback)
        }
    }

    /**
     * Invoke the function to unmute and show optional text associated with the action.
     * @param duration for the view to be in the extended mode before it starts to shrink.
     * @param callback MapboxNavigationConsumer<Boolean> invoked after the animation is finished and
     * returns false representing that view is in unmuted state.
     */
    @JvmOverloads
    fun unmuteAndExtend(duration: Long, callback: MapboxNavigationConsumer<Boolean>? = null) {
        showTextWithAnimation(R.string.mapbox_unmuted, duration) {
            unmute(callback)
        }
    }

    private fun showTextWithAnimation(
        @StringRes text: Int,
        duration: Long,
        beforeAnimation: () -> Unit
    ) {
        beforeAnimation()
        val extendToWidth = EXTEND_TEXT_TO_WIDTH * context.resources.displayMetrics.density
        val animator = getAnimator(textWidth, extendToWidth.toInt())
        mainHandler.removeCallbacksAndMessages(null)
        binding.soundButtonText.extend(
            animator,
            doOnStart = {
                binding.soundButtonText.text = context.getString(text)
                binding.soundButtonText.visibility = View.VISIBLE
                mainHandler.postDelayed(
                    {
                        val endAnimator = getAnimator(extendToWidth.toInt(), textWidth)
                        binding.soundButtonText.shrink(
                            endAnimator,
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
        const val SLIDE_DURATION = 300L
        const val EXTEND_TEXT_TO_WIDTH = 165
    }
}

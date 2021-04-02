package com.mapbox.navigation.ui.maps.guidance.signboard.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardValue
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

/**
 * Default Signboard View that renders snapshot.
 */
class MapboxSignboardView : AppCompatImageView {

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
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

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

    private val ioJobController: JobControl = ThreadController.getIOScopeAndRootJob()
    private val mainJobController: JobControl = ThreadController.getMainScopeAndRootJob()
    private var job: Job? = null

    private val callback = object : MapboxNavigationConsumer<Bitmap?> {
        override fun accept(value: Bitmap?) {
            visibility = if (value != null) {
                setImageBitmap(value)
                VISIBLE
            } else {
                setImageBitmap(null)
                GONE
            }
        }
    }

    /**
     * Invoke to render the signboard based on data or error conditions.
     */
    fun render(result: Expected<SignboardValue, SignboardError>) {
        when (result) {
            is Expected.Success -> {
                renderSignboard(result.value, callback)
            }
            is Expected.Failure -> {
                setImageBitmap(null)
                visibility = GONE
            }
        }
    }

    private fun renderSignboard(
        value: SignboardValue,
        callback: MapboxNavigationConsumer<Bitmap?>
    ) {
        when (value.bytes.isEmpty()) {
            true -> callback.accept(null)
            else -> {
                job?.cancel()
                job = ioJobController.scope.launch {
                    val stream = ByteArrayInputStream(value.bytes)
                    val signboard = SvgUtil.renderAsBitmapWithWidth(
                        stream,
                        value.options.desiredSignboardWidth,
                        value.options.cssStyles
                    )
                    mainJobController.scope.launch {
                        callback.accept(signboard)
                    }
                }
            }
        }
    }
}

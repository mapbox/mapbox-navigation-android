package com.mapbox.androidauto.surfacelayer.textview

import android.graphics.Bitmap
import android.graphics.Rect
import android.opengl.Matrix
import com.mapbox.androidauto.surfacelayer.CarSurfaceLayer
import com.mapbox.androidauto.surfacelayer.GLUtils
import com.mapbox.androidauto.surfacelayer.GLUtils.BYTES_PER_FLOAT
import com.mapbox.androidauto.surfacelayer.toFloatBuffer
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.utils.internal.ifNonNull

class CarTextModel2d : CarSurfaceLayer() {

    val dimensions = COORDS_PER_VERTEX_2D
    val stride = COORDS_PER_VERTEX_2D * BYTES_PER_FLOAT
    val length = VERTEX_COUNT
    val vertices by lazy { VERTEX_COORDS.toFloatBuffer() }
    val textureCords by lazy { TEXTURE_COORDS.toFloatBuffer() }

    /**
     * Transformation matrix describes this model orientation space.
     */
    val modelMatrix = FloatArray(GLUtils.MATRIX_SIZE).also {
        Matrix.setIdentityM(it, 0)
    }

    private var bitmap: Bitmap? = null

    fun updateModelMatrix(nextBitmap: Bitmap?) {
        bitmap = nextBitmap
        ifNonNull(visibleArea, edgeInsets) { visibleArea, edgeInsets ->
            updateModelMatrix(visibleArea, edgeInsets)
        }
    }

    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        super.onVisibleAreaChanged(visibleArea, edgeInsets)

        updateModelMatrix(visibleArea, edgeInsets)
    }

    private fun updateModelMatrix(visibleArea: Rect, edgeInsets: EdgeInsets) {
        Matrix.setIdentityM(modelMatrix, 0)
        val bitmap = bitmap ?: return

        val paddingWidth = padding.left + padding.right
        val paddingHeight = padding.top + padding.bottom
        val width = visibleArea.right - visibleArea.left - paddingWidth
        val height = visibleArea.bottom - visibleArea.top - paddingHeight
        val translateX = edgeInsets.left + padding.left
        val translateY = edgeInsets.top + padding.top

        val scaleX = bitmap.width / width.toFloat()
        val scaleY = bitmap.height / height.toFloat()

        Matrix.translateM(
            modelMatrix, 0,
            translateX.toFloat(),
            translateY.toFloat(),
            0.0f,
        )
        Matrix.translateM(
            modelMatrix, 0,
            (width / 2.0f).toFloat() - bitmap.width / 2.0f,
            height.toFloat() - bitmap.height,
            0.0f,
        )
        Matrix.scaleM(
            modelMatrix, 0,
            width.toFloat() * scaleX,
            height.toFloat() * scaleY,
            1.0f,
        )
    }

    private companion object {

        // Only 2 coordinates because this is a 2d layer with orthographic projection
        private const val COORDS_PER_VERTEX_2D = 2

        // GL_TRIANGLE_STRIP in counterclockwise order
        private val VERTEX_COORDS = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )
        private var TEXTURE_COORDS = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )

        private val VERTEX_COUNT = VERTEX_COORDS.size / COORDS_PER_VERTEX_2D

        private val padding = EdgeInsets(0.0, 0.0, 10.0, 0.0)
    }
}

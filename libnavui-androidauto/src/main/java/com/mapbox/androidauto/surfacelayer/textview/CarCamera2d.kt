package com.mapbox.androidauto.surfacelayer.textview

import android.graphics.Rect
import android.opengl.GLES20
import android.opengl.Matrix
import com.mapbox.androidauto.surfacelayer.CarSurfaceLayer
import com.mapbox.androidauto.surfacelayer.GLUtils
import com.mapbox.maps.EdgeInsets

class CarCamera2d : CarSurfaceLayer() {
    /**
     * Transformation matrix describes the projection orientation space.
     */
    val projM = FloatArray(GLUtils.MATRIX_SIZE)

    /**
     * Transformation matrix describes the view orientation space.
     */
    val viewM = FloatArray(GLUtils.MATRIX_SIZE)

    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        super.onVisibleAreaChanged(visibleArea, edgeInsets)
        val (surfaceWidth, surfaceHeight) = surfaceDimensions() ?: return

        Matrix.setLookAtM(
            viewM, 0,
            EYE[x], EYE[y], EYE[z],
            LOOK_AT[x], LOOK_AT[y], LOOK_AT[z],
            UP[x], UP[y], UP[z]
        )
        GLES20.glViewport(visibleArea.left, visibleArea.top, visibleArea.right, visibleArea.bottom)
        Matrix.orthoM(
            projM, 0,
            CAMERA_LEFT,
            surfaceWidth.toFloat(),
            surfaceHeight.toFloat(),
            CAMERA_TOP,
            CAMERA_NEAR, CAMERA_FAR
        )
    }

    private companion object {
        // Constant camera viewport, changes with the surface dimensions
        private const val CAMERA_LEFT = 0.0f
        private const val CAMERA_TOP = 0.0f
        private const val CAMERA_NEAR = 1.0f
        private const val CAMERA_FAR = 100.0f

        // Constant camera, looking down the z-axis.
        // This makes x,y coordinates the 2 dimensions
        private val EYE = floatArrayOf(0.0f, 0.0f, 10.0f)
        private val LOOK_AT = floatArrayOf(0.0f, 0.0f, 0.0f)
        private val UP = floatArrayOf(0.0f, 1.0f, 0.0f)

        // Indexing constants
        private const val x = 0
        private const val y = 1
        private const val z = 2
    }
}

package com.mapbox.androidauto.surfacelayer.textview

import android.graphics.Rect
import android.opengl.Matrix
import com.mapbox.androidauto.logAndroidAuto
import com.mapbox.androidauto.surfacelayer.CarSurfaceLayer
import com.mapbox.androidauto.surfacelayer.GLUtils
import com.mapbox.maps.EdgeInsets

class CarScene2d : CarSurfaceLayer() {

    val mvpMatrix = FloatArray(GLUtils.MATRIX_SIZE)
    val camera = CarCamera2d()
    val model = CarTextModel2d()

    override fun children() = listOf(camera, model)

    override fun onVisibleAreaChanged(visibleArea: Rect, edgeInsets: EdgeInsets) {
        super.onVisibleAreaChanged(visibleArea, edgeInsets)

        Matrix.multiplyMM(
            mvpMatrix, 0,
            camera.projM, 0,
            camera.viewM, 0
        )

        logAndroidAuto(
            "CarScene2d visibleAreaChanged visibleArea:$visibleArea: edgeInsets:$edgeInsets",
        )
    }
}

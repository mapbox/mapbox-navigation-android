package com.mapbox.navigation.ui.androidauto.navigation.maneuver

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mapbox.navigation.tripdata.maneuver.api.MapboxTurnIconsApi
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.SubManeuver
import com.mapbox.navigation.tripdata.maneuver.model.TurnIconResources
import com.mapbox.navigation.ui.androidauto.internal.RendererUtils.dpToPx
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure

/**
 * Create icons from the Mapbox navigation maneuvers.
 */
class CarManeuverIconRenderer(
    private val options: CarManeuverIconOptions,
) {
    private var turnIconResources = TurnIconResources.Builder().build()
    private val turnIconsApi = MapboxTurnIconsApi(turnIconResources)

    fun renderManeuverIcon(maneuver: PrimaryManeuver): CarIcon? {
        return renderManeuverIcon(
            maneuver.type,
            maneuver.degrees,
            maneuver.modifier,
            maneuver.drivingSide,
        )
    }

    fun renderManeuverIcon(maneuver: SubManeuver): CarIcon? {
        return renderManeuverIcon(
            maneuver.type,
            maneuver.degrees,
            maneuver.modifier,
            maneuver.drivingSide,
        )
    }

    private fun renderManeuverIcon(
        type: String?,
        degrees: Double?,
        modifier: String?,
        drivingSide: String?,
    ): CarIcon? {
        val maneuverTurnIcon =
            turnIconsApi.generateTurnIcon(type, degrees?.toFloat(), modifier, drivingSide)
                .onError {
                    logAndroidAutoFailure(
                        "CarManeuverIconRenderer renderManeuverIcon error ${it.errorMessage}",
                    )
                }
                .value
        val turnIconRes = maneuverTurnIcon?.icon ?: return null
        val bitmap = renderBitmap(turnIconRes, maneuverTurnIcon.shouldFlipIcon)
        return CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
    }

    private fun renderBitmap(@DrawableRes drawableId: Int, shouldFlip: Boolean): Bitmap {
        val vectorDrawable = VectorDrawableCompat.create(
            options.context.resources,
            drawableId,
            ContextThemeWrapper(options.context, options.styleRes).theme,
        )!!

        val px = options.context.dpToPx(CAR_ICON_DIMEN_DP)
        val bitmap: Bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(options.background)
        val canvas = Canvas(bitmap)
        if (shouldFlip) {
            val pivotX = (px / 2.0).toFloat()
            canvas.scale(-1f, 1f, pivotX, 0f)
        }
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }

    private companion object {
        // The current implementation androidx.car.app.navigation.model.Maneuver says it expects 64 x 64
        // https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/car/app/app/src/main/java/androidx/car/app/navigation/model/Maneuver.java#607
        // Although, a future version says 128 so this can be updated.
        private const val CAR_ICON_DIMEN_DP = 64
    }
}

package com.mapbox.navigation.trip.notification

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.mapbox.navigation.utils.internal.SET_BACKGROUND_COLOR

internal class MapboxTripNotificationView(
    private val context: Context,
) {
    var collapsedView: RemoteViews? = null
        private set
    var expandedView: RemoteViews? = null
        private set

    /**
     * Rebuilds the view component references.
     */
    fun buildRemoteViews(pendingCloseIntent: PendingIntent?) {
        val backgroundColor =
            ContextCompat.getColor(context, R.color.mapbox_notification_blue)

        collapsedView = createRemoteView(
            backgroundColor,
            R.layout.mapbox_notification_navigation_collapsed,
            R.id.navigationCollapsedNotificationLayout,
        )

        expandedView = createRemoteView(
            backgroundColor,
            R.layout.mapbox_notification_navigation_expanded,
            R.id.navigationExpandedNotificationLayout,
        ).also {
            it.setOnClickPendingIntent(R.id.endNavigationBtn, pendingCloseIntent)
        }
    }

    /**
     * Sets the visibility of the view components.
     *
     * @param visibility a View.VISIBLE or View.GONE
     */
    fun setVisibility(visibility: Int) {
        updateViewVisibility(
            collapsedView,
            R.id.navigationIsStarting,
            visibility,
        )
        updateViewVisibility(
            expandedView,
            R.id.navigationIsStarting,
            visibility,
        )
    }

    /**
     * Sets the text for the expanded notification remote view.
     *
     * @param textResource the resource ID
     */
    fun setEndNavigationButtonText(textResource: Int) {
        expandedView?.setTextViewText(
            R.id.endNavigationBtn,
            context.getString(textResource),
        )
    }

    /**
     * Sets the instruction text for the collapsed and expanded views.
     *
     * @param primaryText the text resource ID
     */
    fun updateInstructionText(primaryText: String) {
        collapsedView?.setTextViewText(
            R.id.notificationInstructionText,
            primaryText,
        )
        expandedView?.setTextViewText(
            R.id.notificationInstructionText,
            primaryText,
        )
    }

    /**
     * Updates the distance text for the collapsed and expanded views.
     *
     * @param currentDistanceText the text resource ID
     */
    fun updateDistanceText(currentDistanceText: SpannableString?) {
        collapsedView?.setTextViewText(
            R.id.notificationDistanceText,
            currentDistanceText.toString(),
        )
        expandedView?.setTextViewText(
            R.id.notificationDistanceText,
            currentDistanceText.toString(),
        )
    }

    /**
     * Updates the arrival time for the collapsed and expanded views.
     *
     * @param time the time text
     */
    fun updateArrivalTime(time: String) {
        collapsedView?.setTextViewText(R.id.notificationArrivalText, time)
        expandedView?.setTextViewText(R.id.notificationArrivalText, time)
    }

    /**
     * Updates the image for the collapsed and expanded views.
     *
     * @param bitmap the bitmap to apply
     */
    fun updateImage(bitmap: Bitmap) {
        collapsedView?.setImageViewBitmap(R.id.maneuverImage, bitmap)
        expandedView?.setImageViewBitmap(R.id.maneuverImage, bitmap)
    }

    internal fun getImageDrawable(@DrawableRes image: Int): Drawable? {
        return ContextCompat.getDrawable(context, image)
    }

    /**
     * Resets the text for the collapsed and expanded views to empty strings and resets the
     * visibility.
     */
    fun resetView() {
        collapsedView?.apply {
            setTextViewText(R.id.notificationDistanceText, "")
            setTextViewText(R.id.notificationArrivalText, "")
            setTextViewText(R.id.notificationInstructionText, "")
            setViewVisibility(R.id.etaContent, View.GONE)
            setViewVisibility(R.id.notificationInstructionText, View.GONE)
            setViewVisibility(R.id.freeDriveText, View.GONE)
        }

        expandedView?.apply {
            setTextViewText(R.id.notificationDistanceText, "")
            setTextViewText(R.id.notificationArrivalText, "")
            setTextViewText(R.id.notificationInstructionText, "")
            setTextViewText(R.id.endNavigationBtn, "")
            setViewVisibility(R.id.etaContent, View.GONE)
            setViewVisibility(R.id.notificationInstructionText, View.GONE)
            setViewVisibility(R.id.freeDriveText, View.GONE)
        }
    }

    /**
     * Updates the expanded and collapsed views visibility.
     *
     * @param isFreeDriveMode indicates if in free drive
     */
    fun setFreeDriveMode(isFreeDriveMode: Boolean) {
        when (isFreeDriveMode) {
            true -> {
                updateViewVisibility(collapsedView, R.id.etaContent, View.GONE)
                updateViewVisibility(expandedView, R.id.etaContent, View.GONE)
                updateViewVisibility(
                    collapsedView,
                    R.id.notificationInstructionText,
                    View.GONE,
                )
                updateViewVisibility(
                    expandedView,
                    R.id.notificationInstructionText,
                    View.GONE,
                )
                updateViewVisibility(
                    collapsedView,
                    R.id.freeDriveText,
                    View.VISIBLE,
                )
                updateViewVisibility(
                    expandedView,
                    R.id.freeDriveText,
                    View.VISIBLE,
                )
                setImageViewResource(collapsedView)
                setImageViewResource(expandedView)
                setEndNavigationButtonText(R.string.mapbox_stop_session)
            }
            false -> {
                updateViewVisibility(
                    collapsedView,
                    R.id.etaContent,
                    View.VISIBLE,
                )
                updateViewVisibility(expandedView, R.id.etaContent, View.VISIBLE)
                updateViewVisibility(
                    collapsedView,
                    R.id.notificationInstructionText,
                    View.VISIBLE,
                )
                updateViewVisibility(
                    expandedView,
                    R.id.notificationInstructionText,
                    View.VISIBLE,
                )
                updateViewVisibility(
                    collapsedView,
                    R.id.freeDriveText,
                    View.GONE,
                )
                updateViewVisibility(
                    expandedView,
                    R.id.freeDriveText,
                    View.GONE,
                )
                setEndNavigationButtonText(R.string.mapbox_end_navigation)
            }
        }
    }

    private fun updateViewVisibility(remoteView: RemoteViews?, viewId: Int, visibility: Int) {
        remoteView?.setViewVisibility(viewId, visibility)
    }

    private fun createRemoteView(
        backgroundColor: Int,
        layoutResource: Int,
        layoutId: Int,
    ): RemoteViews {
        return RemoteViewsProvider.createRemoteViews(context.packageName, layoutResource)
            .also { remoteViews ->
                remoteViews.setInt(layoutId, SET_BACKGROUND_COLOR, backgroundColor)
            }
    }

    private fun setImageViewResource(remoteView: RemoteViews?) {
        remoteView?.setImageViewResource(
            R.id.maneuverImage,
            R.drawable.mapbox_ic_navigation,
        )
    }
}

package com.mapbox.navigation.ui.maps.snapshotter.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.model.snapshotter.SnapshotState

class MapboxSnapshotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MapboxView<SnapshotState>, AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        visibility = AppCompatImageView.GONE
    }

    override fun render(state: SnapshotState) {
        when (state) {
            is SnapshotState.SnapshotReady -> {
                visibility = AppCompatImageView.VISIBLE
                setImageBitmap(state.bitmap)
            }
            is SnapshotState.SnapshotFailure.SnapshotUnavailable -> {
                setImageBitmap(null)
                visibility = AppCompatImageView.GONE
            }
            is SnapshotState.SnapshotFailure.SnapshotEmpty -> {
                setImageBitmap(null)
                visibility = AppCompatImageView.GONE
            }
            is SnapshotState.SnapshotFailure.SnapshotError -> {
                setImageBitmap(null)
                visibility = AppCompatImageView.GONE
            }
        }
    }
}

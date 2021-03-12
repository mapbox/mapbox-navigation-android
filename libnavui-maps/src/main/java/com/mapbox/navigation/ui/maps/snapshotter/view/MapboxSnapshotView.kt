package com.mapbox.navigation.ui.maps.snapshotter.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.maps.snapshotter.api.MapboxSnapshotterApi
import com.mapbox.navigation.ui.maps.snapshotter.model.SnapshotError
import com.mapbox.navigation.ui.maps.snapshotter.model.SnapshotValue

/**
 * Default Snapshot View that renders snapshot based on [MapboxSnapshotterApi.generateSnapshot]
 */
class MapboxSnapshotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    /**
     * Invoke to render the snapshot based on data or error conditions.
     * @param result Expected<SnapshotValue, SnapshotError>
     */
    fun render(result: Expected<SnapshotValue, SnapshotError>) {
        when (result) {
            is Expected.Success -> {
                visibility = VISIBLE
                setImageBitmap(result.value.snapshot)
            }
            is Expected.Failure -> {
                setImageBitmap(null)
                visibility = GONE
            }
        }
    }
}

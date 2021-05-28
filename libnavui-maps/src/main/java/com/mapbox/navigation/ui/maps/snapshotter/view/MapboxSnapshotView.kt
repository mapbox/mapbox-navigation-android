package com.mapbox.navigation.ui.maps.snapshotter.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.mapbox.bindgen.Expected
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
     * @param result Expected<SnapshotError, SnapshotValue>
     */
    fun render(result: Expected<SnapshotError, SnapshotValue>) {
        result.fold(
            { // error
                setImageBitmap(null)
                visibility = GONE
            },
            { value ->
                visibility = VISIBLE
                setImageBitmap(value.snapshot)
            }
        )
    }
}

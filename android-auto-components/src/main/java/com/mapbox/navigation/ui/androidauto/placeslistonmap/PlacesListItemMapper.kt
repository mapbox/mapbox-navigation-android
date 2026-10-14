package com.mapbox.navigation.ui.androidauto.placeslistonmap

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarLocation
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAuto
import com.mapbox.navigation.ui.androidauto.internal.rowTitle
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement

class PlacesListItemMapper(
    private val placeMarkerRenderer: PlaceMarkerRenderer,
    private val unitType: UnitType,
) {

    fun mapToItemList(
        anchorLocation: Location,
        places: List<PlaceRecord>,
        itemClickListener: PlacesListItemClickListener?,
    ): ItemList = mapToItemList(anchorLocation, places, itemClickListener, withRowImages = false)

    /**
     * Only place list templates draw the place marker of a row. With [withRowImages], each row
     * also shows the marker icon as its image, for the other list templates. Place list templates
     * reject rows with both. Without an [anchorLocation] the rows show no distance.
     */
    internal fun mapToItemList(
        anchorLocation: Location?,
        places: List<PlaceRecord>,
        itemClickListener: PlacesListItemClickListener?,
        withRowImages: Boolean,
    ): ItemList {
        val listBuilder = ItemList.Builder()
        places.forEach { placeRecord ->
            // A row needs a coordinate for its place marker.
            val coordinate = placeRecord.coordinate ?: return@forEach
            // A row can't have an empty title, and names can be empty.
            val title = rowTitle(placeRecord.name, placeRecord.description)
            if (title == null) {
                logAndroidAuto("PlacesListItemMapper skips a place without a name: $placeRecord")
                return@forEach
            }
            val description = rowDescription(anchorLocation, coordinate, placeRecord.description)
            val isBrowsable = false
            val icon = getCarIcon()

            listBuilder.addItem(
                Row.Builder()
                    .setTitle(title)
                    .apply { if (description != null) addText(description) }
                    .setMetadata(
                        Metadata.Builder()
                            .setPlace(
                                Place.Builder(
                                    CarLocation.create(
                                        android.location.Location("").also {
                                            it.latitude = coordinate.latitude()
                                            it.longitude = coordinate.longitude()
                                        },
                                    ),
                                ).setMarker(
                                    PlaceMarker.Builder()
                                        .setIcon(icon, PlaceMarker.TYPE_IMAGE)
                                        .build(),
                                ).build(),
                            )
                            .build(),
                    )
                    .apply { if (withRowImages) setImage(icon) }
                    .setOnClickListener { itemClickListener?.onItemClick(placeRecord) }
                    .setBrowsable(isBrowsable)
                    .build(),
            )
        }

        return listBuilder.build()
    }

    private fun rowDescription(
        anchorLocation: Location?,
        coordinate: Point,
        placeDescription: String?,
    ): CharSequence? {
        if (anchorLocation == null) {
            return placeDescription?.takeIf { it.isNotBlank() }
        }
        val distanceUnits = getDistanceUnits(unitType)
        val distance: Double = TurfMeasurement.distance(
            Point.fromLngLat(anchorLocation.longitude, anchorLocation.latitude),
            coordinate,
            distanceUnits.second,
        )

        // The first character is replaced by the distance span.
        val description = SpannableString(
            if (placeDescription.isNullOrEmpty()) " " else "   \u00b7 $placeDescription",
        )
        description.setSpan(
            DistanceSpan.create(Distance.create(distance, distanceUnits.first)),
            0,
            1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        description.setSpan(
            ForegroundCarColorSpan.create(CarColor.BLUE),
            0,
            1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        return description
    }

    private fun getCarIcon(): CarIcon {
        // fixme this is using a hardcoded icon and is taking for granted it won't be null
        return placeMarkerRenderer.renderMarker()
    }

    private fun getDistanceUnits(unitType: UnitType): Pair<Int, String> {
        return when (unitType) {
            UnitType.METRIC -> {
                Pair(Distance.UNIT_KILOMETERS, TurfConstants.UNIT_KILOMETERS)
            }
            UnitType.IMPERIAL -> {
                Pair(Distance.UNIT_MILES, TurfConstants.UNIT_MILES)
            }
        }
    }
}

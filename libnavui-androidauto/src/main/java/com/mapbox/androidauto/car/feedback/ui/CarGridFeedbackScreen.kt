package com.mapbox.androidauto.car.feedback.ui

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.androidauto.R
import com.mapbox.androidauto.car.feedback.core.CarFeedbackSender

/**
 * This screen allows the user to search for a destination.
 */
class CarGridFeedbackScreen constructor(
    carContext: CarContext,
    private val sourceScreenSimpleName: String,
    private val carFeedbackSender: CarFeedbackSender,
    private val feedbackItems: List<CarFeedbackItem>,
    private val encodedSnapshot: String?,
    private val onFinish: () -> Unit,
) : Screen(carContext) {

    private var selectedItem: CarFeedbackItem? = null

    override fun onGetTemplate(): Template {
        return GridTemplate.Builder()
            .setTitle(carContext.resources.getString(R.string.car_feedback_title))
            .setActionStrip(feedbackActionStrip())
            .setSingleList(buildItemList(carContext))
            .build()
    }

    private fun feedbackActionStrip() = ActionStrip.Builder()
        .addAction(Action.Builder().setIcon(CarIcon.BACK).setOnClickListener(onFinish).build())
        .addAction(
            Action.Builder()
                .setTitle(carContext.getString(R.string.car_feedback_submit))
                .setOnClickListener {
                    val selectedItem = this.selectedItem
                    if (selectedItem == null) {
                        CarToast.makeText(
                            carContext,
                            carContext.getString(R.string.car_feedback_submit_toast_select_item),
                            CarToast.LENGTH_LONG
                        ).show()
                    } else {
                        carFeedbackSender.send(selectedItem, encodedSnapshot, sourceScreenSimpleName)
                        CarToast.makeText(
                            carContext,
                            carContext.getString(R.string.car_feedback_submit_toast_success),
                            CarToast.LENGTH_LONG
                        ).show()
                        onFinish()
                    }
                }
                .build()
        )
        .build()

    private fun buildItemList(carContext: CarContext): ItemList {
        val itemListBuilder = ItemList.Builder()
        feedbackItems.map { gridItem ->

            val iconDrawableRes = gridItem.carFeedbackIcon.drawableRes()
            val icon = CarIcon.Builder(IconCompat.createWithResource(carContext, iconDrawableRes))
                .also {
                    if (selectedItem == gridItem) {
                        it.setTint(CarColor.BLUE)
                    }
                }
                .build()
            GridItem.Builder()
                .setTitle(gridItem.carFeedbackTitle)
                .setImage(icon, GridItem.IMAGE_TYPE_ICON)
                .setOnClickListener {
                    selectedItem = gridItem
                    invalidate()
                }
                .build()
        }.forEach {
            itemListBuilder.addItem(it)
        }
        return itemListBuilder.build()
    }
}

package com.mapbox.navigation.magpt.ui.cards

import com.mapbox.navigation.mapgpt.core.api.CardComponent.Image
import com.mapbox.navigation.mapgpt.core.api.CardComponent.Spacer
import com.mapbox.navigation.mapgpt.core.api.CardComponent.Stack
import com.mapbox.navigation.mapgpt.core.api.CardComponent.Text
import com.mapbox.navigation.mapgpt.core.api.SessionFrame.SendEvent.Body.Entity.Data.Card
import com.mapbox.navigation.mapgpt.core.api.SessionFrame

class GenericCardStubs {
    companion object {

        private const val MAPGPT_ASSETS_URL = "https://api.mapbox.com/mapgpt-assets/"

        private const val SOUND_WAVE_ASSET_LIGHT = "soundwave_light.png"
        private const val SOUND_WAVE_ASSET_DARK = "soundwave_dark.png"
        private const val TRIPADVISER_LOGO_ASSET_LIGHT = "tripadviser_logo_light.png"
        private const val TRIPADVISER_LOGO_ASSET_DARK = "tripadviser_logo_dark.png"
        private const val RATING_ASSET = "rating_4.5.png"
        private const val RATING_ASSET_ASPECT_RATIO = 3.411f
        private const val MUSIC_ARTWORK_ASSET = "https://www.rollingstone.com/wp-content/uploads" +
            "/2018/06/rs-28119-20140415-acdc-x1800-1397607291.jpg"
        private const val RESTAURANT_ASSET = "https://images.unsplash.com/" +
            "photo-1517248135467-4c7edcad34c4?w=400"

        val genericCardStub = SessionFrame.SendEvent.Body.Entity.Data.Card(
            listOf(
                Image(
                    "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=400",
                    uriDark = null,
                    aspectRatio = 1f,
                ),
                Stack(
                    topRow = listOf(
                        Text(
                            "Restaurant name",
                            "title7",
                            "primary",
                        ),
                        Spacer(),
                        Text(
                            "144 mi",
                            "body5",
                            "secondary",
                        ),
                    ),
                    bottomRow = listOf(
                        Text(
                            "Subtitle placeholder",
                            "body5",
                            "secondary",
                        ),
                    ),
                ),
            ),
            anchoredComponent = null,
            payload = null,
        )

        val genericCardStubWithRating = Card(
            listOf(
                Image(
                    RESTAURANT_ASSET,
                    uriDark = null,
                    aspectRatio = 1f,
                ),
                Stack(
                    topRow = listOf(
                        Text(
                            "Restaurant name",
                            "title7",
                            "primary",
                        ),
                        Spacer(),
                        Text(
                            "144 mi",
                            "body5",
                            "secondary",
                        ),
                    ),
                    bottomRow = listOf(
                        Image(
                            MAPGPT_ASSETS_URL + TRIPADVISER_LOGO_ASSET_LIGHT,
                            uriDark = MAPGPT_ASSETS_URL + TRIPADVISER_LOGO_ASSET_DARK,
                            aspectRatio = null,
                        ),
                        Image(
                            MAPGPT_ASSETS_URL + RATING_ASSET,
                            uriDark = null,
                            aspectRatio = RATING_ASSET_ASPECT_RATIO,
                        ),
                        Text(
                            content = "35",
                            fontSize = "body5",
                            fontColor = "secondary",
                        )
                    ),
                ),
            ),
            anchoredComponent = Image(
                uriLight = "https://api.mapbox.com/mapgpt-assets/service_opentable.png",
                uriDark = null,
                aspectRatio = null,
            ),
            payload = null,
        )

        val genericCardStubWithLongName = Card(
            listOf(
                Image(
                    RESTAURANT_ASSET,
                    uriDark = null,
                    aspectRatio = 1f,
                ),
                Stack(
                    topRow = listOf(
                        Text(
                            "Restaurant name Restaurant name Restaurant name",
                            "title7",
                            "primary",
                        ),
                        Spacer(),
                        Text(
                            "144 mi",
                            "body5",
                            "secondary",
                        ),
                    ),
                    bottomRow = listOf(
                        Image(
                            MAPGPT_ASSETS_URL + TRIPADVISER_LOGO_ASSET_LIGHT,
                            uriDark = MAPGPT_ASSETS_URL + TRIPADVISER_LOGO_ASSET_DARK,
                            aspectRatio = null,
                        ),
                        Image(
                            MAPGPT_ASSETS_URL + RATING_ASSET,
                            uriDark = null,
                            aspectRatio = RATING_ASSET_ASPECT_RATIO,
                        ),
                    ),
                ),
            ),
            anchoredComponent = null,
            payload = null,
        )

        val musicCard = Card(
            listOf(
                Image(
                    MUSIC_ARTWORK_ASSET,
                    uriDark = null,
                    aspectRatio = 1f,
                ),
                Stack(
                    topRow = listOf(
                        Text(
                            "Highway to Hell",
                            "title7",
                            "primary",
                        ),
                    ),
                    bottomRow = listOf(
                        Text(
                            "AC/DC",
                            "body5",
                            "secondary",
                        ),
                    ),
                ),
                Spacer(),
            ),
            anchoredComponent = null,
            payload = null,
        )

        val musicCardWithSoundWave = Card(
            listOf(
                Image(
                    MUSIC_ARTWORK_ASSET,
                    uriDark = null,
                    aspectRatio = 1f,
                ),
                Stack(
                    topRow = listOf(
                        Text(
                            "Highway to Hell",
                            "title7",
                            "primary",
                        ),
                    ),
                    bottomRow = listOf(
                        Text(
                            "AC/DC",
                            "body5",
                            "secondary",
                        ),
                    ),
                ),
                Spacer(),
                Image(
                    uriLight = SOUND_WAVE_ASSET_LIGHT,
                    uriDark = SOUND_WAVE_ASSET_DARK,
                    aspectRatio = 1f,
                ),
            ),
            anchoredComponent = null,
            payload = null,
        )

        val genericCardHvac = Card(
            listOf(
                Text(
                    content = "75",
                    fontSize = "title3",
                    fontColor = "primary",
                ),
                Stack(
                    topRow = listOf(
                        Text(
                            content = "˚F",
                            fontSize = "title7",
                            fontColor = "primary",
                        ),
                        Spacer(),
                        Text(
                            content = "Temperature",
                            fontSize = "title7",
                            fontColor = "primary",
                        ),
                    ),
                    bottomRow = null,
                ),
            ),
            anchoredComponent = null,
            payload = null,
        )

        val genericCardWithInvalidAssets = Card(
            listOf(
                // Local asset that doesn't exist
                Image(
                    "mapgpt-asset://this_asset_doesn't_exist.png",
                    uriDark = null,
                    aspectRatio = 1f,
                ),
                // URL that doesn't exist
                Image(
                    "https://this.url.doesnt.exist.by",
                    uriDark = null,
                    aspectRatio = 1f,
                ),
                // This URL exists but doesn't have a valid image
                Image(
                    "https://google.com",
                    uriDark = null,
                    aspectRatio = 1f,
                ),
            ),
            anchoredComponent = null,
            payload = null,
        )
    }
}

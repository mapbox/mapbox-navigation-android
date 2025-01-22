package com.mapbox.navigation.ui.maps.guidance.signboard.api

import android.graphics.Bitmap
import com.caverock.androidsvg.SVGExternalFileResolver
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import com.mapbox.navigation.utils.internal.ByteBufferBackedInputStream
import java.nio.ByteBuffer

/**
 * Implementation of [SvgToBitmapParser] that provides a method allowing to convert SVG
 * [ByteArray] to [Bitmap]
 */
class MapboxSvgToBitmapParser(
    private val externalFileResolver: SVGExternalFileResolver,
) : SvgToBitmapParser {

    /**
     * The function converts a raw svg in [ByteArray] to [Bitmap] using [MapboxSignboardOptions]
     * specified.
     *
     * @param svg raw data in [ByteArray]
     * @param options additional option to format the svg.
     * @return [Expected] contains [Bitmap] if successful or error otherwise.
     */
    override fun parse(
        svg: ByteBuffer,
        options: MapboxSignboardOptions,
    ): Expected<String, Bitmap> {
        return try {
            val stream = ByteBufferBackedInputStream(svg)
            ExpectedFactory.createValue(
                stream.use {
                    SvgUtil.renderAsBitmapWithWidth(
                        it,
                        options.desiredSignboardWidth,
                        options.cssStyles,
                        externalFileResolver,
                    )
                },
            )
        } catch (ex: Exception) {
            ExpectedFactory.createError(ex.message ?: "")
        }
    }
}

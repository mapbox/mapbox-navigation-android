package com.mapbox.services.android.navigation.v5.utils.distance;

import android.graphics.Typeface;
import android.location.Location;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;
import com.mapbox.services.android.navigation.v5.utils.span.SpanItem;
import com.mapbox.services.android.navigation.v5.utils.span.SpanUtils;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfConversion;
import com.mapbox.turf.TurfMeasurement;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by selimyildiz on 12/11/2017.
 */

public class MeterDistanceUtils extends DistanceUtils {

    private static final String METER_FORMAT = " m";
    private static final String KILOMETER_FORMAT = " km";

    @Override
    public SpannableStringBuilder distanceFormatterBold(double distance,
                                                        DecimalFormat decimalFormat, boolean spansEnabled) {
        SpannableStringBuilder formattedString;
        if (longDistance(distance)) {
            formattedString = roundToNearest(distance, spansEnabled);
        } else if (mediumDistance(distance)) {
            formattedString = roundOneDecimalPlace(distance, decimalFormat, spansEnabled);
        } else {
            formattedString = roundBySmallDistance(distance, spansEnabled);
        }
        return formattedString;
    }

    @Override
    public SpannableStringBuilder roundBySmallDistance(double distance, boolean spansEnabled) {
        // Distance value
        int roundedNumber = ((int) Math.round(distance)) / 50 * 50;
        roundedNumber = roundedNumber < 50 ? 50 : roundedNumber;

        if (spansEnabled) {
            return generateSpannedText(String.valueOf(roundedNumber), METER_FORMAT);
        } else {
            return new SpannableStringBuilder(String.valueOf(roundedNumber) + METER_FORMAT);
        }
    }

    @Override
    public SpannableStringBuilder roundOneDecimalPlace(double distance,
                                                       DecimalFormat decimalFormat, boolean spansEnabled) {
        // Distance value
        double roundedNumber = (distance / 100 * 100);
        String roundedDecimal = decimalFormat.format(roundedNumber);

        if (spansEnabled) {
            return generateSpannedText(roundedDecimal, KILOMETER_FORMAT);
        } else {
            return new SpannableStringBuilder(roundedDecimal + KILOMETER_FORMAT);
        }
    }

    @Override
    public SpannableStringBuilder roundToNearest(double distance, boolean spansEnabled) {
        // Distance value
        int roundedNumber = (int) Math.round(distance);

        if (spansEnabled) {
            return generateSpannedText(String.valueOf(roundedNumber), KILOMETER_FORMAT);
        } else {
            return new SpannableStringBuilder(String.valueOf(roundedNumber) + KILOMETER_FORMAT);
        }
    }

    @Override
    public boolean mediumDistance(double distance) {
        return TurfConversion.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_KILOMETERS) < 10
                && distance > 401;
    }

    @Override
    public boolean longDistance(double distance) {
        return TurfConversion.convertDistance(distance, TurfConstants.UNIT_METERS, TurfConstants.UNIT_KILOMETERS) > 10;
    }


    @Override
    public SpannableStringBuilder generateSpannedText(String distance, String unit) {
        List<SpanItem> spans = new ArrayList<>();
        spans.add(new SpanItem(new StyleSpan(Typeface.BOLD), distance));
        spans.add(new SpanItem(new RelativeSizeSpan(0.65f), unit));
        return SpanUtils.combineSpans(spans);
    }

}

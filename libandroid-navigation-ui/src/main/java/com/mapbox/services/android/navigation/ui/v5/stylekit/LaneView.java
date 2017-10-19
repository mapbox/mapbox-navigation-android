package com.mapbox.services.android.navigation.ui.v5.stylekit;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;

import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneMap;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.*;
import com.mapbox.services.api.directions.v5.models.IntersectionLanes;

import java.util.ArrayList;
import java.util.Collection;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_NONE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_SHARP_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_SHARP_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_SLIGHT_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_SLIGHT_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_STRAIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.TURN_LANE_INDICATION_UTURN;

public class LaneView extends View {


    private IntersectionLanes intersectionLanes;
    private ManeuverModifier maneuverModifier;
    private String laneIndications;
    int primaryColor = Color.BLACK;
    int secondaryColor = Color.LTGRAY;
    TurnLaneMap turnLaneMap;
    RectF rect;

    public LaneView(Context context) {
        super(context);
        init(null, 0);
        turnLaneMap = new TurnLaneMap();
    }

    public LaneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public LaneView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    public void setLane(IntersectionLanes lanes) {
        this.intersectionLanes = lanes;
        rect = new RectF(0, 0, getWidth(), getHeight());


        StringBuilder builder = new StringBuilder();
        // Indications
        if (lanes.getIndications() != null) {
            for (String indication : lanes.getIndications()) {
                builder.append(indication);
            }
        }

        laneIndications = builder.toString();

        invalidate();
    }

    public void setManeuverModifier(ManeuverModifier maneuverModifier) {
        this.maneuverModifier = maneuverModifier;
        invalidate();
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.LaneView, defStyle, 0);

        // TODO: Fix
//        this.primaryColor = a.getColor(R.styleable.ManeuverView_primaryColor, Color.BLACK);
//        this.secondaryColor = a.getColor(R.styleable.ManeuverView_secondaryColor, Color.LTGRAY);

        a.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: Move this out of `onDraw`
        if (rect.width() != getWidth() || rect.height() != getHeight()) {
            rect = new RectF(0, 0, getWidth(), getHeight());
        }
        LanesStyleKit.ResizingBehavior resizingBehavior = LanesStyleKit.ResizingBehavior.AspectFit;

        //LanesStyleKit.drawLane_straight(canvas, rect, resizingBehavior, primaryColor);

        //if lane.indications.isSuperset(of: [.straightAhead, .sharpRight]) || lane.indications.isSuperset(of: [.straightAhead, .right]) || lane.indications.isSuperset(of: [.straightAhead, .slightRight])

        String turnLaneResourceKey = laneIndications + maneuverModifier;
        int drawLaneKey = turnLaneMap.getTurnLaneResource(turnLaneResourceKey);

        switch (drawLaneKey) {

        }


//
//
//
//        if (isInEditMode()) {
//
//        }
//
//        if (lane == null) {
//            return;
//        }
//
//        if (lane.getIndications().toString().equals(TURN_LANE_INDICATION_LEFT)) {
//
//        }


    }
}

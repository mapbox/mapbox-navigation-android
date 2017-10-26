package com.mapbox.services.android.navigation.ui.v5.stylekit;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.mapbox.services.android.navigation.ui.v5.R;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.*;


/**
 * A view that draws a maneuver arrow indicating the upcoming maneuver.
 *
 * @since 0.6.0
 */
public class ManeuverView extends View {

    @ManeuverType
    String maneuverType = null;

    @ManeuverModifier
    String maneuverModifier = null;

    int primaryColor = Color.BLACK;
    int secondaryColor = Color.LTGRAY;

    public ManeuverView(Context context) {
        super(context);
        init(null, 0);
    }

    public ManeuverView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ManeuverView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.ManeuverView, defStyle, 0);

        this.primaryColor = a.getColor(R.styleable.ManeuverView_primaryColor, Color.BLACK);
        this.secondaryColor = a.getColor(R.styleable.ManeuverView_secondaryColor, Color.LTGRAY);

        a.recycle();
    }

    public void setManeuverType(String maneuverType) {
        this.maneuverType = maneuverType;
        invalidate();

    }

    public void setManeuverModifier(String maneuverModifier) {
        this.maneuverModifier = maneuverModifier;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: Move to outside of onDraw()
        RectF rect = new RectF(0, 0, getWidth(), getHeight());
        PointF size = new PointF(getWidth(), getHeight());


        ManeuversStyleKit.ResizingBehavior resizingBehavior = ManeuversStyleKit.ResizingBehavior.AspectFit;
        if (isInEditMode()) {

            ManeuversStyleKit.drawArrow0(canvas, rect, resizingBehavior, primaryColor, size);
            return;
        }

        if (maneuverType == null || maneuverModifier == null) {
            return;
        }


        boolean flip = false;

        switch (maneuverType) {
            case STEP_MANEUVER_TYPE_MERGE:
                ManeuversStyleKit.drawMerge(canvas, rect, resizingBehavior, primaryColor, secondaryColor, size);
                flip = shouldFlip(maneuverModifier);
                break;

            case STEP_MANEUVER_TYPE_OFF_RAMP:
                ManeuversStyleKit.drawOfframp(canvas, rect, resizingBehavior, primaryColor, secondaryColor, size);
                flip = shouldFlip(maneuverModifier);
                break;

            case STEP_MANEUVER_TYPE_FORK:
                ManeuversStyleKit.drawFork(canvas, rect, resizingBehavior, primaryColor, secondaryColor, size);
                flip = shouldFlip(maneuverModifier);
                break;

            case STEP_MANEUVER_TYPE_ROUNDABOUT:
            case STEP_MANEUVER_TYPE_ROUNDABOUT_TURN:
            case STEP_MANEUVER_TYPE_ROTARY:
                switch (maneuverModifier) {
                    case STEP_MANEUVER_MODIFIER_STRAIGHT:
                        ManeuversStyleKit.drawRoundabout(canvas, rect, resizingBehavior, primaryColor, secondaryColor, size, 180);
                        break;

                    case STEP_MANEUVER_MODIFIER_SLIGHT_LEFT:
                    case STEP_MANEUVER_MODIFIER_LEFT:
                    case STEP_MANEUVER_MODIFIER_SHARP_LEFT:
                        ManeuversStyleKit.drawRoundabout(canvas, rect, resizingBehavior, primaryColor, secondaryColor, size, 275);
                        break;

                    default:
                        ManeuversStyleKit.drawRoundabout(canvas, rect, resizingBehavior, primaryColor, secondaryColor, size, 90);
                }
                break;

            case STEP_MANEUVER_TYPE_ARRIVE:
                switch (maneuverModifier) {
                    case STEP_MANEUVER_MODIFIER_RIGHT:
                        ManeuversStyleKit.drawArriveright2(canvas, rect, resizingBehavior, primaryColor, size);
                        flip = false;
                        break;

                    case STEP_MANEUVER_MODIFIER_LEFT:
                        ManeuversStyleKit.drawArriveright2(canvas, rect, resizingBehavior, primaryColor, size);
                        flip = true;
                        break;

                    default:
                        ManeuversStyleKit.drawArriveright2(canvas, rect, resizingBehavior, primaryColor, size);
                }
                break;

            default:
                switch (maneuverModifier) {
                    case STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT:
                        ManeuversStyleKit.drawArrow30(canvas, rect, resizingBehavior, primaryColor, size);
                        flip = false;
                        break;

                    case STEP_MANEUVER_MODIFIER_RIGHT:
                        ManeuversStyleKit.drawArrow45(canvas, rect, resizingBehavior, primaryColor, size);
                        flip = false;
                        break;

                    case STEP_MANEUVER_MODIFIER_SHARP_RIGHT:
                        ManeuversStyleKit.drawArrow75(canvas, rect, resizingBehavior, primaryColor, size);
                        flip = false;
                        break;

                    case STEP_MANEUVER_MODIFIER_SLIGHT_LEFT:
                        ManeuversStyleKit.drawArrow30(canvas, rect, resizingBehavior, primaryColor, size);
                        flip = true;
                        break;

                    case STEP_MANEUVER_MODIFIER_LEFT:
                        ManeuversStyleKit.drawArrow45(canvas, rect, resizingBehavior, primaryColor, size);
                        flip = true;
                        break;

                    case STEP_MANEUVER_MODIFIER_SHARP_LEFT:
                        ManeuversStyleKit.drawArrow75(canvas, rect, resizingBehavior, primaryColor, size);
                        flip = true;
                        break;

                    case STEP_MANEUVER_MODIFIER_UTURN:
                        ManeuversStyleKit.drawArrow180(canvas, rect, resizingBehavior, primaryColor, size);
                        break;

                    default:
                        ManeuversStyleKit.drawArrow0(canvas, rect, resizingBehavior, primaryColor, size);
                }
        }

        setScaleX(flip ? -1 : 1);
    }

    private boolean shouldFlip(String modifier) {
        return modifier.contains(STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT)
                || modifier.contains(STEP_MANEUVER_MODIFIER_RIGHT)
                || modifier.contains(STEP_MANEUVER_MODIFIER_SHARP_RIGHT);
    }
}

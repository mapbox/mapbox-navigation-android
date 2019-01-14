package com.mapbox.services.android.navigation.testapp.example.ui.callout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.BubbleLayout;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.testapp.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.string;
import static com.mapbox.mapboxsdk.style.layers.Property.ICON_ANCHOR_BOTTOM;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;

public class ExampleCalloutManager {

  private static final String SOURCE_ID = "mapbox_nav_callout_source_id";
  private static final String LAYER_ID = "mapbox_nav_callout_source_id";

  private long id;
  private MapboxMap mapboxMap;
  private GeoJsonSource geoJsonSource;
  private List<Feature> featureList = new ArrayList<>();

  public ExampleCalloutManager(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    this.geoJsonSource = new GeoJsonSource(SOURCE_ID);
    mapboxMap.getStyle().addSource(geoJsonSource);
    mapboxMap.getStyle().addLayer(new SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
      iconImage(string(get(literal(ExampleCallout.KEY_ID)))),
      iconAllowOverlap(true),
      iconIgnorePlacement(true),
      iconAnchor(ICON_ANCHOR_BOTTOM)
    ));
  }

  public ExampleCallout add(ExampleCalloutOptions options) {
    ExampleCallout callout = options.build(id++);
    featureList.add(callout.toFeature());
    new GenerateSymbolTask(mapboxMap, Mapbox.getApplicationContext()).execute(callout);
    updateSource();
    return callout;
  }

  public void removeAll() {
    featureList.clear();
    updateSource();
  }

  private void updateSource() {
    geoJsonSource.setGeoJson(FeatureCollection.fromFeatures(featureList));
  }

  private static class GenerateSymbolTask extends AsyncTask<ExampleCallout, Void, HashMap<String, Bitmap>> {

    private MapboxMap mapboxMap;
    private WeakReference<Context> context;

    GenerateSymbolTask(MapboxMap mapboxMap, Context context) {
      this.mapboxMap = mapboxMap;
      this.context = new WeakReference<>(context);
    }

    @SuppressWarnings("WrongThread")
    @Override
    protected HashMap<String, Bitmap> doInBackground(ExampleCallout... params) {
      HashMap<String, Bitmap> imagesMap = new HashMap<>();
      Context context = this.context.get();
      if (context != null && params != null) {
        ExampleCallout callout = params[0];
        if (callout instanceof ExampleBubbleCallout) {
          imagesMap.put(callout.getId(), generateBubbleText(context, (ExampleBubbleCallout) callout));
        } else {
          imagesMap.put(callout.getId(), generateText(context, callout));
        }
      }
      return imagesMap;
    }

    private Bitmap generateBubbleText(Context context, ExampleBubbleCallout bubbleCallout) {
      BubbleLayout bubbleLayout = (BubbleLayout) LayoutInflater.from(context).inflate(R.layout.callout, null);
      bubbleLayout.setCornersRadius(bubbleCallout.getCornerRadius());
      bubbleLayout.setStrokeColor(bubbleCallout.getStrokeColor());
      bubbleLayout.setStrokeWidth(bubbleCallout.getStrokeWidth());
      bubbleLayout.setBubbleColor(bubbleCallout.getBackgroundColor());
      bubbleLayout.setArrowHeight(bubbleCallout.getArrowHeight());
      bubbleLayout.setArrowWidth(bubbleCallout.getArrowWidth());
      int[] padding = bubbleCallout.getPadding();
      bubbleLayout.setPadding(padding[0], padding[1], padding[2], padding[3]);

      TextView textView = bubbleLayout.findViewById(R.id.title);
      textView.setText(bubbleCallout.getText());
      textView.setTextColor(bubbleCallout.getTextColor());
      textView.setTextSize(bubbleCallout.getTextSize());

      // determine placement of arrow
      int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
      bubbleLayout.measure(measureSpec, measureSpec);
      int measuredWidth = bubbleLayout.getMeasuredWidth();
      bubbleLayout.setArrowPosition((measuredWidth - bubbleCallout.getArrowWidth()) / 2);
      return SymbolGenerator.generate(bubbleLayout);
    }

    private Bitmap generateText(Context context, ExampleCallout callout) {
      TextView textView = new TextView(context);
      textView.setBackgroundColor(callout.getBackgroundColor());
      textView.setTextColor(callout.getTextColor());
      textView.setText(callout.getText());
      textView.setTextSize(callout.getTextSize());
      int[] padding = callout.getPadding();
      textView.setPadding(padding[0], padding[1], padding[2], padding[3]);
      return SymbolGenerator.generate(textView);
    }

    @Override
    protected void onPostExecute(HashMap<String, Bitmap> bitmapHashMap) {
      super.onPostExecute(bitmapHashMap);
      mapboxMap.getStyle().addImages(bitmapHashMap);
    }
  }

  private static class SymbolGenerator {

    /**
     * Generate a Bitmap from an Android SDK View.
     *
     * @param view the View to be drawn to a Bitmap
     * @return the generated bitmap
     */
    static Bitmap generate(@NonNull View view) {
      int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
      view.measure(measureSpec, measureSpec);

      int measuredWidth = view.getMeasuredWidth();
      int measuredHeight = view.getMeasuredHeight();

      view.layout(0, 0, measuredWidth, measuredHeight);
      Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
      bitmap.eraseColor(Color.TRANSPARENT);
      Canvas canvas = new Canvas(bitmap);
      view.draw(canvas);
      return bitmap;
    }
  }
}

package org.blitzortung.android.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.helper.ViewHelper;
import org.blitzortung.android.data.beans.RasterParameters;
import org.blitzortung.android.map.overlay.StrikesOverlay;
import org.blitzortung.android.map.overlay.color.ColorHandler;

public class LegendView extends View {


    public static final float REGION_HEIGHT = 1.1f;
    public static final float RASTER_HEIGHT = 0.8f;
    private float width;
    private float height;

    final private float padding;
    final private float colorFieldSize;
    private float textWidth;

    final private Paint textPaint;
    final private Paint rasterTextPaint;
    final private Paint regionTextPaint;
    final private Paint backgroundPaint;
    final private Paint foregroundPaint;

    private StrikesOverlay strikesOverlay;

    private final RectF backgroundRect;
    private final RectF legendColorRect;

    @SuppressWarnings("unused")
    public LegendView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("unused")
    public LegendView(Context context) {
        this(context, null, 0);
    }

    public LegendView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        padding = ViewHelper.pxFromSp(this, 5);
        colorFieldSize = ViewHelper.pxFromSp(this, 12);

        foregroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(context.getResources().getColor(R.color.translucent_background));

        backgroundRect = new RectF();
        legendColorRect = new RectF();

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xffffffff);
        textPaint.setTextSize(colorFieldSize);

        rasterTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rasterTextPaint.setColor(0xffffffff);
        rasterTextPaint.setTextSize(colorFieldSize * RASTER_HEIGHT);
        rasterTextPaint.setTextAlign(Paint.Align.CENTER);

        regionTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        regionTextPaint.setColor(0xffffffff);
        regionTextPaint.setTextSize(colorFieldSize * REGION_HEIGHT);
        regionTextPaint.setTextAlign(Paint.Align.CENTER);

        updateTextWidth(0);

        setBackgroundColor(Color.TRANSPARENT);
    }

    private void updateTextWidth(int intervalDuration) {
        textWidth = (float) Math.ceil(textPaint.measureText(intervalDuration > 100 ? "< 100min" : "< 10min"));
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        updateTextWidth(strikesOverlay.getIntervalDuration());
        width = Math.min(3 * padding + colorFieldSize + textWidth, parentWidth);

        ColorHandler colorHandler = strikesOverlay.getColorHandler();

        height = Math.min((colorFieldSize + padding) * colorHandler.getColors().length + padding, parentHeight);

        if (hasRegion()) {
            height += colorFieldSize * REGION_HEIGHT + padding;
        }

        if (hasRaster()) {
            height += colorFieldSize * RASTER_HEIGHT + padding;
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec((int) width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int) height, MeasureSpec.EXACTLY));
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (strikesOverlay != null) {
            ColorHandler colorHandler = strikesOverlay.getColorHandler();
            int minutesPerColor = strikesOverlay.getIntervalDuration() / colorHandler.getNumberOfColors();

            backgroundRect.set(0, 0, width, height);
            canvas.drawRect(backgroundRect, backgroundPaint);

            int numberOfColors = colorHandler.getNumberOfColors();

            float topCoordinate = padding;

            for (int index = 0; index < numberOfColors; index++) {
                foregroundPaint.setColor(colorHandler.getColor(index));
                legendColorRect.set(padding, topCoordinate, padding + colorFieldSize, topCoordinate + colorFieldSize);
                canvas.drawRect(legendColorRect, foregroundPaint);

                boolean isLastValue = index == numberOfColors - 1;
                String text = String.format("%c %dmin", isLastValue ? '>' : '<', (index + (isLastValue ? 0 : 1)) * minutesPerColor);

                canvas.drawText(text, 2 * padding + colorFieldSize, topCoordinate + colorFieldSize / 1.1f, textPaint);

                topCoordinate += colorFieldSize + padding;
            }

            if (hasRegion()) {
                canvas.drawText(getRegionName(), width / 2.0f, topCoordinate + colorFieldSize * REGION_HEIGHT / 1.1f, regionTextPaint);
                topCoordinate += colorFieldSize * REGION_HEIGHT + padding;
            }

            if (hasRaster()) {
                canvas.drawText("Raster: " + getRasterString(), width / 2.0f, topCoordinate + colorFieldSize * RASTER_HEIGHT / 1.1f, rasterTextPaint);
                topCoordinate += colorFieldSize * RASTER_HEIGHT + padding;
            }
        }
    }

    private String getRegionName() {
        int regionNumber = strikesOverlay.getRegion();

        int index = 0;
        for (String region_number : getResources().getStringArray(R.array.regions_values)) {
            if (regionNumber == Integer.parseInt(region_number)) {
                String region = getResources().getStringArray(R.array.regions)[index];
                return region;
            }
            index++;
        }

        return "n/a";
    }

    public void setStrikesOverlay(StrikesOverlay strikesOverlay) {
        this.strikesOverlay = strikesOverlay;
    }

    public void setAlpha(int alpha) {
        foregroundPaint.setAlpha(alpha);
    }

    private boolean hasRaster() {
        return strikesOverlay.hasRasterParameters();
    }

    private boolean hasRegion() {
        return strikesOverlay.getRegion() != 0;
    }

    public String getRasterString() {
        RasterParameters rasterParameters = strikesOverlay.getRasterParameters();
        return rasterParameters.getInfo();
    }
}

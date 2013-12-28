package org.blitzortung.android.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.helper.ViewHelper;
import org.blitzortung.android.data.DataListener;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.map.overlay.StrokesOverlay;
import org.blitzortung.android.map.overlay.color.ColorHandler;

public class HistogramView extends View implements DataListener {

    private float width;
    private float height;

    final private float padding;
    final private float textSize;

    final private Paint backgroundPaint;
    final private Paint foregroundPaint;
    final private Paint textPaint;

    private StrokesOverlay strokesOverlay;

    private int[] histogram;

    private final int defaultForegroundColor;
    private final RectF backgroundRect;

    @SuppressWarnings("unused")
    public HistogramView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("unused")
    public HistogramView(Context context) {
        this(context, null, 0);
    }

    public HistogramView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        padding = ViewHelper.pxFromDp(this, 5);
        textSize = ViewHelper.pxFromSp(this, 12);

        foregroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(context.getResources().getColor(R.color.translucent_background));

        defaultForegroundColor = context.getResources().getColor(R.color.text_foreground);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(defaultForegroundColor);
        textPaint.setTextSize(textSize);
        textPaint.setTextAlign(Paint.Align.RIGHT);

        backgroundRect = new RectF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        width = parentWidth;
        height = parentHeight;

        super.onMeasure(MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(parentHeight, MeasureSpec.EXACTLY));
    }

    @Override
    public void onDraw(Canvas canvas) {

        if (strokesOverlay != null && histogram != null && histogram.length > 0) {
            ColorHandler colorHandler = strokesOverlay.getColorHandler();
            int minutesPerColor = strokesOverlay.getIntervalDuration() / colorHandler.getNumberOfColors();
            int minutesPerBin = 5;
            int ratio = minutesPerColor / minutesPerBin;
            if (ratio == 0) {
                return;
            }

            backgroundRect.set(0, 0, width, height);
            canvas.drawRect(backgroundRect, backgroundPaint);

            int maximumCount = 0;
            for (int count : histogram) {
                if (count > maximumCount) {
                    maximumCount = count;
                }
            }

            canvas.drawText(String.format("%.1f/min _", (float)maximumCount/ minutesPerBin), width - 2*padding, padding + textSize/1.2f, textPaint);

            int ymax = maximumCount == 0 ? 1 : maximumCount;

            float x0 = padding;
            float xd = (width - 2 * padding) / (histogram.length - 1);

            float y0 = height - padding;
            float yd = (height - 2 * padding - textSize) / ymax;

            foregroundPaint.setStrokeWidth(2);
            for (int i = 0; i < histogram.length - 1; i++) {
                foregroundPaint.setColor(colorHandler.getColor((histogram.length - 1 - i) / ratio));
                canvas.drawLine(x0 + xd * i, y0 - yd * histogram[i], x0 + xd * (i+1), y0 - yd * histogram[i+1], foregroundPaint);
            }

            foregroundPaint.setStrokeWidth(1);
            foregroundPaint.setColor(defaultForegroundColor);

            canvas.drawLine(padding, height - padding, width - padding, height - padding, foregroundPaint);
            canvas.drawLine(width - padding, padding, width - padding, height - padding, foregroundPaint);
        }
    }

    public void setStrokesOverlay(StrokesOverlay strokesOverlay) {
        this.strokesOverlay = strokesOverlay;
    }

    @Override
    public void onBeforeDataUpdate() {
    }

    @Override
    public void onDataUpdate(DataResult result) {
        histogram = result.getHistogram();

        boolean viewShouldBeVisible = histogram != null && histogram.length > 0;

        setVisibility(viewShouldBeVisible ? View.VISIBLE : View.INVISIBLE);

        if (viewShouldBeVisible) {
            invalidate();
        }
    }

    @Override
    public void onDataReset() {
        histogram = new int[0];

        setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDataError() {
        onDataReset();
    }
}

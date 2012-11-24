package org.blitzortung.android.app.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import org.blitzortung.android.app.R;
import org.blitzortung.android.map.overlay.StrokesOverlay;
import org.blitzortung.android.map.overlay.color.ColorHandler;

public class LegendView extends View {


    private float width;
    private float height;

    final private float padding;
    final private float colorFieldSize;
    private float textWidth;

    final private Paint textPaint;
    final private Paint backgroundPaint;
    final private Paint foregroundPaint;

    private StrokesOverlay strokesOverlay;

    public LegendView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LegendView(Context context) {
        this(context, null, 0);
    }

    public LegendView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        padding = pxFromDp(4);
        colorFieldSize = pxFromDp(8);

        foregroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(context.getResources().getColor(R.color.translucent_background));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xffffffff);
        textPaint.setTextSize(colorFieldSize);

        updateTextWidth(0);

        setBackgroundColor(Color.TRANSPARENT);
    }

    private void updateTextWidth(int intervalDuration) {
        textWidth = (float)Math.ceil(textPaint.measureText(intervalDuration > 100 ? "< 100min" : "< 10min"));
    }

    private float pxFromDp(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        updateTextWidth(strokesOverlay.getIntervalDuration());
        Log.w("LegendVier", String.format("interval %d", strokesOverlay.getIntervalDuration()));
        width = Math.min(3 * padding + colorFieldSize + textWidth, parentWidth);

        if (strokesOverlay != null) {
            ColorHandler colorHandler = strokesOverlay.getColorHandler();

            height = Math.min((colorFieldSize + padding) * colorHandler.getColors().length + padding, parentHeight);
        } else {
            height = Math.min(10, parentHeight);
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec((int)width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int)height, MeasureSpec.EXACTLY));
    }

    @Override
    public void onDraw(Canvas canvas) {
        //super.onDraw(canvas);

        if (strokesOverlay != null) {
            ColorHandler colorHandler = strokesOverlay.getColorHandler();
            int minutesPerColor = strokesOverlay.getIntervalDuration() / colorHandler.getNumberOfColors();

            RectF backgroundRect = new RectF(0, 0, width, height);
            canvas.drawRect(backgroundRect, backgroundPaint);

            int numberOfColors = colorHandler.getNumberOfColors();
            for (int index = 0; index < numberOfColors; index++) {
                foregroundPaint.setColor(colorHandler.getColor(index));
                float topCoordinate = padding + (colorFieldSize + padding) * index;
                RectF rect = new RectF(padding, topCoordinate, padding + colorFieldSize, topCoordinate + colorFieldSize);
                canvas.drawRect(rect, foregroundPaint);

                boolean isLastValue = index == numberOfColors - 1;
                String text = String.format("%c %dmin", isLastValue ? '>' : '<', (index + (isLastValue ? 0 : 1)) * minutesPerColor);

                canvas.drawText(text, 2 * padding + colorFieldSize, topCoordinate + colorFieldSize / 1.1f, textPaint);
            }

        }
    }

    public void setStrokesOverlay(StrokesOverlay strokesOverlay) {
        this.strokesOverlay = strokesOverlay;
    }

    public void setAlpha(int alpha) {
        foregroundPaint.setAlpha(alpha);
    }
}

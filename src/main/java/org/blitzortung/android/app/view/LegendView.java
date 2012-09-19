package org.blitzortung.android.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import org.blitzortung.android.app.R;
import org.blitzortung.android.map.overlay.StrokesOverlay;
import org.blitzortung.android.map.overlay.color.ColorHandler;

public class LegendView extends View {


    private int width;
    private int height;

    final private int padding = 5;
    final private int colorFieldSize = 12;
    final private int colorFieldSeparator = 5;
    final private int textWidth;

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

        foregroundPaint = new Paint();

        backgroundPaint = new Paint();
        backgroundPaint.setColor(context.getResources().getColor(R.color.translucent_background));

        textPaint = new Paint();
        textPaint.setColor(0xffffffff);
        textPaint.setTextSize(colorFieldSize);

        textWidth = (int)Math.ceil(textPaint.measureText("< 10min"));
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        width = Math.min(2 * padding + colorFieldSize + textWidth + colorFieldSeparator, parentWidth);

        if (strokesOverlay != null) {
            ColorHandler colorHandler = strokesOverlay.getColorHandler();

            height = Math.min((colorFieldSize + colorFieldSeparator) * colorHandler.getColors().length + padding, parentHeight);
        } else {
            height = Math.min(10, parentHeight);
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (strokesOverlay != null) {
            ColorHandler colorHandler = strokesOverlay.getColorHandler();
            int minutesPerColor = strokesOverlay.getMinutesPerColor();

            RectF backgroundRect = new RectF(0, 0, width, height);
            canvas.drawRect(backgroundRect, backgroundPaint);

            int numberOfColors = colorHandler.getNumberOfColors();
            for (int index = 0; index < numberOfColors; index++) {
                foregroundPaint.setColor(colorHandler.getColor(index));
                int topCoordinate = padding + (colorFieldSize + colorFieldSeparator) * index;
                RectF rect = new RectF(padding, topCoordinate, padding + colorFieldSize, topCoordinate + colorFieldSize);
                canvas.drawRect(rect, foregroundPaint);

                boolean isLastValue = index == numberOfColors - 1;
                String text = String.format("%c %dmin", isLastValue ? '>' : '<', (index + (isLastValue ? 0 : 1)) * minutesPerColor);

                canvas.drawText(text, padding + colorFieldSize + colorFieldSeparator, topCoordinate + colorFieldSize / 1.1f, textPaint);
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

package org.blitzortung.android.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import org.blitzortung.android.map.overlay.color.ColorHandler;

public class LegendView extends View {


    private int width;
    private int height;

    private ColorHandler colorHandler;
    private int alpha;

    public LegendView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LegendView(Context context) {
        this(context, null, 0);
    }

    public LegendView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        alpha=255;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        width = Math.min(20, parentWidth);

        if (colorHandler != null) {
            height = Math.min(10 * colorHandler.getColors().length + 5, parentHeight);
        } else {
            height = Math.min(10, parentHeight);
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (colorHandler != null) {
            Paint color = new Paint();
            color.setColor(0xff666666);
            color.setAlpha(alpha);
            RectF bgrect = new RectF(0, 0, width, height);
            canvas.drawRect(bgrect, color);

            for (int index = 0; index < colorHandler.getColors().length; index++) {
                color.setColor(colorHandler.getColor(index));
                color.setAlpha(alpha);
                RectF rect = new RectF(5, 5 + 10 * index, 15, 10 + 10 * index);
                canvas.drawRect(rect, color);
            }
        }
    }

    public void setColorHandler(ColorHandler colorHandler) {
        this.colorHandler = colorHandler;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }
}

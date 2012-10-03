package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class RasterShapeTest {

    @Mock
    private Canvas canvas;

    @Mock
    private Paint paint;

    @Mock
    private Point center;

    @Mock
    private Point topRight;

    @Mock
    private Point bottomLeft;

    private RasterShape rasterShape;

    private int color = 0x102030;

    private int multiplicity = 5;

    private int textColor = 0xa0b0c0;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        rasterShape = new RasterShape(center, topRight, bottomLeft, color, multiplicity, textColor);
    }

    @Test
    public void testDraw()
    {
        rasterShape.draw(canvas, paint);

        verify(paint, times(1)).setColor(color);
        verify(paint, times(1)).setColor(textColor);
        verify(paint, times(2)).setAlpha(anyInt());
        verify(canvas, times(1)).drawRect(any(RectF.class), eq(paint));
        verify(paint, times(1)).setTextAlign(Paint.Align.CENTER);
        verify(paint, times(1)).setTextSize(anyFloat());
        verify(canvas, times(1)).drawText(eq(String.valueOf(multiplicity)), anyFloat(), anyFloat(), eq(paint));
    }

    @Test
    public void testAlpha() {
        assertFalse(rasterShape.hasAlpha());
    }

}

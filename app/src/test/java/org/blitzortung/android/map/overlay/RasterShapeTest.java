package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 19)
public class RasterShapeTest {

    @Mock
    private Canvas canvas;

    @Mock
    private Paint paint;

    private Point topLeft = new Point(-3, -3);

    private Point bottomRight = new Point(3, 3);

    private RasterShape rasterShape;

    private final int color = 0x102030;

    private final int multiplicity = 5;

    private final int textColor = 0xa0b0c0;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        rasterShape = new RasterShape();
        rasterShape.update(topLeft, bottomRight, color, multiplicity, textColor);
    }

    @Test
    public void testDraw()
    {
        rasterShape.draw(canvas, paint);

        verify(paint, times(1)).setColor(color);
        verify(paint, times(1)).setAlpha(anyInt());
        verify(canvas, times(1)).drawRect(any(RectF.class), eq(paint));

        verify(paint, times(0)).setColor(textColor);
        verify(paint, times(0)).setTextAlign(Paint.Align.CENTER);
        verify(paint, times(0)).setTextSize(anyFloat());
        verify(canvas, times(0)).drawText(eq(String.valueOf(multiplicity)), anyFloat(), anyFloat(), eq(paint));
    }

    @Ignore
    public void testDrawWithMultiplicityText()
    {
        topLeft = new Point(-4,-4);
        bottomRight = new Point(4,4);

        rasterShape = new RasterShape();
        rasterShape.update(topLeft, bottomRight, color, multiplicity, textColor);

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

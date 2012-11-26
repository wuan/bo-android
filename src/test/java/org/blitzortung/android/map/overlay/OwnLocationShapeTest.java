package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class OwnLocationShapeTest {

    @Mock
    private Canvas canvas;

    @Mock
    private Paint paint;

    private OwnLocationShape ownLocationShape;

    private final int size = 12;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ownLocationShape = new OwnLocationShape(size);
    }

    @Test
    public void testDraw()
    {
        ownLocationShape.draw(canvas, paint);

        verify(paint, times(2)).setStyle(any(Paint.Style.class));
        verify(paint, times(3)).setColor(anyInt());
        verify(canvas, times(2)).drawLine(anyFloat(), anyFloat(), anyFloat(), anyFloat(), eq(paint));
        verify(canvas, times(2)).drawCircle(anyFloat(), anyFloat(), anyFloat(), eq(paint));
    }

}

package org.blitzortung.android.map.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class ParticipantShapeTest {

    @Mock
    private Canvas canvas;

    @Mock
    private Paint paint;

    private ParticipantShape participantShape;

    private int color = 0x102030;

    private int size = 12;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        participantShape = new ParticipantShape(size, color);
    }

    @Test
    public void testDraw()
    {
        participantShape.draw(canvas, paint);

        verify(paint, times(1)).setColor(color);
        verify(canvas, times(1)).drawRect(anyFloat(), anyFloat(), anyFloat(), anyFloat(), eq(paint));
    }

}

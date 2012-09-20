package org.blitzortung.android.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.alarm.AlarmSector;
import org.blitzortung.android.alarm.AlarmStatus;
import org.blitzortung.android.app.R;
import org.blitzortung.android.map.overlay.color.ColorHandler;

public class AlarmView extends View implements AlarmManager.AlarmListener {

    public static final int TEXT_REQUIRED_SIZE = 300;
    private AlarmManager alarmManager;

    private AlarmStatus alarmStatus;

    private ColorHandler colorHandler;

    private int minutesPerColor;

    private final RectF arcArea = new RectF();
    private final Paint background = new Paint();
    private final Paint sectorPaint = new Paint();
    private final Paint lines = new Paint();
    private final Paint textStyle = new Paint();
    private final Paint warnText = new Paint();

    private final String[] alarmNotAvailableTextLines;

    public AlarmView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlarmView(Context context) {
        this(context, null, 0);
    }

    public AlarmView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        alarmNotAvailableTextLines = context.getString(R.string.alarms_not_available).split("\n");

        background.setColor(0xffc0c0c0);
    }


    public void setAlarmManager(AlarmManager alarmManager) {
        this.alarmManager = alarmManager;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        int size = Math.min(parentWidth, parentHeight);

        super.onMeasure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onAttachedToWindow() {
        alarmManager.addAlarmListener(this);
        alarmStatus = alarmManager.getAlarmStatus();
    }

    @Override
    protected void onDetachedFromWindow() {
        alarmManager.removeAlarmListener(this);
        alarmStatus = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int size = Math.max(getWidth(), getHeight());
        int pad = 4;

        float center = size / 2.0f;
        float radius = center - pad;

        arcArea.set(pad, pad, size - pad, size - pad);

        lines.setColor(0xff404040);
        lines.setStyle(Style.STROKE);
        lines.setStrokeWidth(size / 150);
        lines.setAntiAlias(true);

        textStyle.setColor(0xff404040);
        textStyle.setTextAlign(Align.CENTER);

        canvas.drawArc(arcArea, 0, 360, true, background);

        if (alarmStatus != null) {
            long actualTime = System.currentTimeMillis();

            float sectorSize = alarmStatus.getSectorSize();

            float radiusIncrement = radius / AlarmSector.getDistanceStepCount();

            for (int sectorIndex = 0; sectorIndex < alarmStatus.getSectorCount(); sectorIndex++) {
                float bearing = alarmStatus.getSectorBearing(sectorIndex) + 90 + 180;
                AlarmSector sector = alarmStatus.getSector(sectorIndex);

                int counts[] = sector.getEventCount();
                long latestTimes[] = sector.getLatestTimes();
                for (int distanceIndex = AlarmSector.getDistanceStepCount() - 1; distanceIndex >= 0; distanceIndex--) {

                    float sectorRadius = (distanceIndex + 1) * radiusIncrement;
                    float leftTop = center - sectorRadius;
                    float bottomRight = center + sectorRadius;
                    float startAngle = bearing - sectorSize / 2.0f;

                    if (counts[distanceIndex] > 0) {
                        sectorPaint.setColor(colorHandler.getColor(actualTime, latestTimes[distanceIndex], minutesPerColor));
                    } else {
                        sectorPaint.setColor(background.getColor());
                    }
                    arcArea.set(leftTop, leftTop, bottomRight, bottomRight);
                    canvas.drawArc(arcArea, startAngle, sectorSize, true, sectorPaint);
                }
            }

            if (size > TEXT_REQUIRED_SIZE) {
                for (int sectorIndex = 0; sectorIndex < alarmStatus.getSectorCount(); sectorIndex++) {
                    double bearing = alarmStatus.getSectorBearing(sectorIndex);
                    canvas.drawLine(center, center, center + (float) (radius * Math.sin((bearing + sectorSize / 2.0f) / 180.0f * Math.PI)), center
                            + (float) (radius * -Math.cos((bearing + sectorSize / 2.0f) / 180.0f * Math.PI)), lines);

                    String text = alarmStatus.getSectorLabel(sectorIndex);
                    if (!text.equals("O")) {
                        float textRadius = (AlarmSector.getDistanceStepCount() - 0.5f) * radiusIncrement;
                        canvas.drawText(text, center
                                + (float) (textRadius * Math.sin(bearing / 180.0 * Math.PI)), center
                                + (float) (textRadius * -Math.cos(bearing / 180.0 * Math.PI)) + textStyle.getFontMetrics(null) / 3f, textStyle);
                    }
                }

                textStyle.setTextAlign(Align.RIGHT);
                for (int distanceIndex = 0; distanceIndex < AlarmSector.getDistanceStepCount(); distanceIndex++) {
                    float leftTop = center - (distanceIndex + 1) * radiusIncrement;
                    float bottomRight = center + (distanceIndex + 1) * radiusIncrement;
                    arcArea.set(leftTop, leftTop, bottomRight, bottomRight);
                    canvas.drawArc(arcArea, 0, 360, false, lines);

                    String text = String.format("%.0f", AlarmSector.getDistanceSteps()[distanceIndex] / 1000);
                    canvas.drawText(text, center + (distanceIndex + 0.95f) * radiusIncrement, center
                            + textStyle.getFontMetrics(null) / 3f, textStyle);
                }
            }

        } else if (size > TEXT_REQUIRED_SIZE) {

            warnText.setColor(0xffa00000);
            warnText.setTextAlign(Align.CENTER);
            warnText.setTextSize(20);
            warnText.setAntiAlias(true);

            for (int line = 0; line < alarmNotAvailableTextLines.length; line++) {
                canvas.drawText(alarmNotAvailableTextLines[line], center, center + (line - 1) * warnText.getFontMetrics(null), warnText);
            }
        }

    }

    @Override
    public void onAlarmResult(AlarmStatus alarmStatus) {
        this.alarmStatus = alarmStatus;
        invalidate();
    }

    @Override
    public void onAlarmClear() {
        alarmStatus = null;
    }

    public void setColorHandler(ColorHandler colorHandler, int minutesPerColor) {
        this.colorHandler = colorHandler;
        this.minutesPerColor = minutesPerColor;
    }

    public void setBackground(int backgroundColor) {
        background.setColor(backgroundColor);
    }
}

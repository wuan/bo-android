package org.blitzortung.android.app.view;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.alarm.AlarmSector;
import org.blitzortung.android.alarm.AlarmStatus;
import org.blitzortung.android.app.R;
import org.blitzortung.android.map.overlay.color.ColorHandler;

public class AlarmView extends View implements AlarmManager.AlarmListener {

    private static final int TEXT_MINIMUM_SIZE = 300;
    private static final PorterDuffXfermode XFERMODE_CLEAR = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private static final PorterDuffXfermode XFERMODE_SRC = new PorterDuffXfermode(PorterDuff.Mode.SRC);
    private AlarmManager alarmManager;

    private AlarmStatus alarmStatus;

    private ColorHandler colorHandler;

    private int intervalDuration;

    private final RectF arcArea = new RectF();
    private final Paint background = new Paint();
    private final Paint sectorPaint = new Paint();
    private final Paint lines = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textStyle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint warnText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint transfer = new Paint();

    private final String[] alarmNotAvailableTextLines;

    private Bitmap temporaryBitmap;
    private Canvas temporaryCanvas;

    @SuppressWarnings("unused")
    public AlarmView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("unused")
    public AlarmView(Context context) {
        this(context, null, 0);
    }

    public AlarmView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        alarmNotAvailableTextLines = context.getString(R.string.alarms_not_available).split("\n");

        lines.setColor(0xff404040);
        lines.setStyle(Style.STROKE);

        textStyle.setColor(0xff404040);

        background.setColor(0xffb0b0b0);
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
        if (alarmManager != null) {
            alarmManager.addAlarmListener(this);
            alarmStatus = alarmManager.getAlarmStatus();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (alarmManager != null) {
            alarmManager.removeAlarmListener(this);
        }
        alarmStatus = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int size = Math.max(getWidth(), getHeight());
        int pad = 4;

        float center = size / 2.0f;
        float radius = center - pad;

        prepareTemporaryBitmap(size);

        Log.v("BO_ANDROID", String.format("AlarmView.onDraw %s", alarmStatus));
        
        if (alarmStatus != null) {
            lines.setColor(colorHandler.getLineColor());
            lines.setStrokeWidth(size / 150);

            textStyle.setTextAlign(Align.CENTER);
            textStyle.setColor(colorHandler.getLineColor());

            long actualTime = System.currentTimeMillis();

            float sectorWidth = alarmStatus.getSectorWidth();

            float radiusIncrement = radius / AlarmSector.getDistanceStepCount();

            for (int sectorIndex = 0; sectorIndex < alarmStatus.getSectorCount(); sectorIndex++) {
                float bearing = alarmStatus.getSectorBearing(sectorIndex) + 90 + 180;
                AlarmSector sector = alarmStatus.getSector(sectorIndex);

                for (int distanceIndex = AlarmSector.getDistanceStepCount() - 1; distanceIndex >= 0; distanceIndex--) {

                    float sectorRadius = (distanceIndex + 1) * radiusIncrement;
                    float leftTop = center - sectorRadius;
                    float bottomRight = center + sectorRadius;
                    float startAngle = bearing - sectorWidth / 2.0f;

                    boolean drawColor = sector.getStrokeCount(distanceIndex) > 0;
                    if (drawColor) {
                        sectorPaint.setColor(colorHandler.getColor(actualTime, sector.getLatestTime(distanceIndex), intervalDuration));
                    }
                    arcArea.set(leftTop, leftTop, bottomRight, bottomRight);
                    temporaryCanvas.drawArc(arcArea, startAngle, sectorWidth, true, drawColor ? sectorPaint : background);
                }
            }


            for (int sectorIndex = 0; sectorIndex < alarmStatus.getSectorCount(); sectorIndex++) {
                double bearing = alarmStatus.getSectorBearing(sectorIndex);
                temporaryCanvas.drawLine(center, center, center + (float) (radius * Math.sin((bearing + sectorWidth / 2.0f) / 180.0f * Math.PI)), center
                        + (float) (radius * -Math.cos((bearing + sectorWidth / 2.0f) / 180.0f * Math.PI)), lines);

                if (size > TEXT_MINIMUM_SIZE) {
                    drawSectorLabel(center, radiusIncrement, sectorIndex, bearing);
                }
            }


            textStyle.setTextAlign(Align.RIGHT);
            float textHeight = textStyle.getFontMetrics(null);
            for (int distanceIndex = 0; distanceIndex < AlarmSector.getDistanceStepCount(); distanceIndex++) {
                float leftTop = center - (distanceIndex + 1) * radiusIncrement;
                float bottomRight = center + (distanceIndex + 1) * radiusIncrement;
                arcArea.set(leftTop, leftTop, bottomRight, bottomRight);
                temporaryCanvas.drawArc(arcArea, 0, 360, false, lines);

                if (size > TEXT_MINIMUM_SIZE) {
                    String text = String.format("%.0f", AlarmSector.getDistanceSteps()[distanceIndex]);
                    temporaryCanvas.drawText(text, center + (distanceIndex + 0.95f) * radiusIncrement, center
                            + textHeight / 3f, textStyle);
                    if (distanceIndex == AlarmSector.getDistanceStepCount() - 1) {
                        temporaryCanvas.drawText(alarmStatus.getSector(0).getDistanceUnitName(), center + (distanceIndex + 0.95f) * radiusIncrement, center
                                + textHeight * 1.33f, textStyle);
                    }
                }
            }

        } else if (size > TEXT_MINIMUM_SIZE) {

            warnText.setColor(0xffa00000);
            warnText.setTextAlign(Align.CENTER);
            warnText.setTextSize(20);

            for (int line = 0; line < alarmNotAvailableTextLines.length; line++) {
                temporaryCanvas.drawText(alarmNotAvailableTextLines[line], center, center + (line - 1) * warnText.getFontMetrics(null), warnText);
            }
        }

        canvas.drawBitmap(temporaryBitmap, 0, 0, transfer);
    }

    private void drawSectorLabel(float center, float radiusIncrement, int sectorIndex, double bearing) {
        String text = alarmStatus.getSectorLabel(sectorIndex);
        if (!text.equals("O")) {
            float textRadius = (AlarmSector.getDistanceStepCount() - 0.5f) * radiusIncrement;
            temporaryCanvas.drawText(text, center
                    + (float) (textRadius * Math.sin(bearing / 180.0 * Math.PI)), center
                    + (float) (textRadius * -Math.cos(bearing / 180.0 * Math.PI)) + textStyle.getFontMetrics(null) / 3f, textStyle);
        }
    }

    private void prepareTemporaryBitmap(int size) {
        if (temporaryBitmap == null) {
            temporaryBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            temporaryCanvas = new Canvas(temporaryBitmap);
        } else {
            background.setXfermode(XFERMODE_CLEAR);
            temporaryCanvas.drawPaint(background);
        }
        background.setXfermode(XFERMODE_SRC);
    }

    @Override
    public void onAlarmResult(AlarmStatus alarmStatus) {
        this.alarmStatus = alarmStatus;
        Log.v("BO_ANDROID", String.format("AlarmView.onAlarmResult %s", alarmStatus));
        invalidate();
    }

    @Override
    public void onAlarmClear() {
        alarmStatus = null;
        Log.v("BO_ANDROID", "AlarmView.onAlarmClear");
        invalidate();
    }

    public void setColorHandler(ColorHandler colorHandler, int intervalDuration) {
        this.colorHandler = colorHandler;
        this.intervalDuration = intervalDuration;
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        background.setColor(backgroundColor);
    }

    public void setAlpha(int alpha) {
        transfer.setAlpha(alpha);
    }

}

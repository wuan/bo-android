package org.blitzortung.android.app.view;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;
import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.AlarmResult;
import org.blitzortung.android.alarm.object.AlarmSector;
import org.blitzortung.android.alarm.object.AlarmSectorRange;
import org.blitzortung.android.alarm.object.AlarmStatus;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.helper.ViewHelper;
import org.blitzortung.android.map.color.ColorHandler;

import java.util.List;

public class AlarmView extends View implements AlarmManager.AlarmListener {

    private static final int TEXT_MINIMUM_SIZE = 300;
    private static final PorterDuffXfermode XFERMODE_CLEAR = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    private static final PorterDuffXfermode XFERMODE_SRC = new PorterDuffXfermode(PorterDuff.Mode.SRC);

    private AlarmManager alarmManager;

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
        textStyle.setTextSize(ViewHelper.pxFromSp(this, 10));
        
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
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (alarmManager != null) {
            alarmManager.removeAlarmListener(this);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int size = Math.max(getWidth(), getHeight());
        int pad = 4;

        float center = size / 2.0f;
        float radius = center - pad;

        prepareTemporaryBitmap(size);

        AlarmStatus alarmStatus = alarmManager.getAlarmStatus();

        if (alarmStatus != null) {
            AlarmParameters alarmParameters = alarmManager.getAlarmParameters();

            final float[] rangeSteps = alarmParameters.getRangeSteps();
            final int rangeStepCount = rangeSteps.length;
            final float radiusIncrement = radius / rangeStepCount;
            final float sectorWidth = 360 / alarmParameters.getSectorLabels().length;

            lines.setColor(colorHandler.getLineColor());
            lines.setStrokeWidth(size / 150);

            textStyle.setTextAlign(Align.CENTER);
            textStyle.setColor(colorHandler.getTextColor());

            long actualTime = System.currentTimeMillis();

            for (AlarmSector alarmSector : alarmStatus.getSectors()) {

                float startAngle = alarmSector.getMinimumSectorBearing() + 90 + 180;

                final List<AlarmSectorRange> ranges = alarmSector.getRanges();
                for (int rangeIndex = ranges.size() - 1; rangeIndex >= 0; rangeIndex--) {
                    AlarmSectorRange alarmSectorRange = ranges.get(rangeIndex);

                    float sectorRadius = (rangeIndex + 1) * radiusIncrement;
                    float leftTop = center - sectorRadius;
                    float bottomRight = center + sectorRadius;

                    boolean drawColor = alarmSectorRange.getStrokeCount() > 0;
                    if (drawColor) {
                        final int color = colorHandler.getColor(actualTime, alarmSectorRange.getLatestStrokeTimestamp(), intervalDuration);
                        sectorPaint.setColor(color);
                    }
                    arcArea.set(leftTop, leftTop, bottomRight, bottomRight);
                    temporaryCanvas.drawArc(arcArea, startAngle, sectorWidth, true, drawColor ? sectorPaint : background);
                }
            }

            for (AlarmSector alarmSector : alarmStatus.getSectors()) {
                double bearing = alarmSector.getMinimumSectorBearing();
                temporaryCanvas.drawLine(center, center, center + (float) (radius * Math.sin(bearing / 180.0f * Math.PI)), center
                        + (float) (radius * -Math.cos(bearing / 180.0f * Math.PI)), lines);

                if (size > TEXT_MINIMUM_SIZE) {
                    drawSectorLabel(center, radiusIncrement, alarmSector, bearing + sectorWidth / 2.0);
                }
            }

            textStyle.setTextAlign(Align.RIGHT);
            float textHeight = textStyle.getFontMetrics(null);
            for (int radiusIndex = 0; radiusIndex < rangeStepCount; radiusIndex++) {
                float leftTop = center - (radiusIndex + 1) * radiusIncrement;
                float bottomRight = center + (radiusIndex + 1) * radiusIncrement;
                arcArea.set(leftTop, leftTop, bottomRight, bottomRight);
                temporaryCanvas.drawArc(arcArea, 0, 360, false, lines);

                if (size > TEXT_MINIMUM_SIZE) {
                    String text = String.format("%.0f", rangeSteps[radiusIndex]);
                    temporaryCanvas.drawText(text, center + (radiusIndex + 0.85f) * radiusIncrement, center
                            + textHeight / 3f, textStyle);
                    if (radiusIndex == rangeStepCount - 1) {
                        temporaryCanvas.drawText(alarmParameters.getMeasurementSystem().getUnitName(), center + (radiusIndex + 0.85f) * radiusIncrement, center
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

    private void drawSectorLabel(float center, float radiusIncrement, AlarmSector sector, double bearing) {
        if (bearing != 90.0) {
            final String text = sector.getLabel();
            float textRadius = (sector.getRanges().size() - 0.5f) * radiusIncrement;
            temporaryCanvas.drawText(text, center
                    + (float) (textRadius * Math.sin(bearing / 180.0 * Math.PI)), center
                    + (float) (textRadius * -Math.cos(bearing / 180.0 * Math.PI)) + textStyle.getFontMetrics(null) / 3f, textStyle);
        }
    }

    private void prepareTemporaryBitmap(int size) {
        if (temporaryBitmap == null) {
            temporaryBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            temporaryCanvas = new Canvas(temporaryBitmap);
        }
        background.setColor(colorHandler.getBackgroundColor());
        background.setXfermode(XFERMODE_CLEAR);
        temporaryCanvas.drawPaint(background);

        background.setXfermode(XFERMODE_SRC);
    }

    @Override
    public void onAlarmResult(AlarmResult alarmResult) {
        invalidate();
    }

    @Override
    public void onAlarmClear() {
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

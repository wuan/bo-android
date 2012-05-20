package org.blitzortung.android.app.view;

import java.util.GregorianCalendar;

import org.blitzortung.android.alarm.AlarmManager;
import org.blitzortung.android.alarm.AlarmSector;
import org.blitzortung.android.alarm.AlarmStatus;
import org.blitzortung.android.app.R;
import org.blitzortung.android.map.overlay.color.ColorHandler;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class AlarmView extends View implements AlarmManager.AlarmListener {

	public AlarmView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AlarmView(Context context) {
		this(context, null, 0);
	}

	public AlarmView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// Log.v("AlarmView", "created");
	}

	private AlarmManager alarmManager;

	private AlarmStatus alarmStatus;

	private ColorHandler colorHandler;

	private int minutesPerColor;

	public void setAlarmManager(AlarmManager alarmManager) {
		// Log.v("AlarmView", "setAlarmManager " + alarmManager);
		this.alarmManager = alarmManager;
		this.alarmStatus = alarmManager.getAlarmStatus();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
		int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

		// Log.v("AlarmView", String.format("onMeasure() width: %d, height: %d",
		// parentWidth, parentHeight));

		int size = Math.min(parentWidth, parentHeight);

		super.onMeasure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));
	}

	@Override
	protected void onAttachedToWindow() {
		alarmManager.addAlarmListener(this);
		// Log.v("AlarmView", "attached");
	}

	@Override
	protected void onDetachedFromWindow() {
		alarmManager.removeAlarmListener(this);
		// Log.v("AlarmView", "detached");
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		int size = Math.max(getWidth(), getHeight());
		int pad = 4;

		float center = size / 2.0f;
		float radius = center - pad;

		// Log.v("AlarmView", String.format("onDraw() size: %d", size));

		RectF area = new RectF(pad, pad, size - pad, size - pad);

		Paint background = new Paint();
		background.setColor(0xffc0c0c0);

		Paint lines = new Paint();
		lines.setColor(0xff404040);
		lines.setStyle(Style.STROKE);
		lines.setStrokeWidth(2);
		lines.setAntiAlias(true);
		
		Paint textStyle = new Paint();
		textStyle.setColor(0xff404040);
		textStyle.setTextAlign(Align.CENTER);

		canvas.drawArc(area, 0, 360, true, background);

		if (alarmStatus != null) {
			long now = new GregorianCalendar().getTime().getTime();

			float sectorSize = alarmStatus.getSectorSize();
			float halfSectorSize = sectorSize / 2.0f;

			float radiusIncrement = radius / AlarmSector.getDistanceLimitCount();

			for (int sectorIndex = 0; sectorIndex < alarmStatus.getSectorCount(); sectorIndex++) {
				float bearing = alarmStatus.getSectorBearing(sectorIndex) + 90 + 180;
				AlarmSector sector = alarmStatus.getSector(sectorIndex);

				// Log.v("AlarmView", String.format("sector %d, bearing %.0f",
				// sectorIndex, bearing));

				int counts[] = sector.getEventCounts();
				long latestTimes[] = sector.getLatestTimes();
				for (int distanceIndex = AlarmSector.getDistanceLimitCount() - 1; distanceIndex >= 0; distanceIndex--) {
					Paint sectorPaint = new Paint();
					float sectorRadius = (distanceIndex + 1) * radiusIncrement;
					float leftTop = center - sectorRadius;
					float bottomRight = center + sectorRadius;
					float startAngle = bearing - halfSectorSize;

					if (counts[distanceIndex] > 0) {
						sectorPaint.setColor(colorHandler.getColor(now, latestTimes[distanceIndex], minutesPerColor));
						// Log.v("AlarmView",
						// String.format("segment %d, count %d, alarm: %.1f, %.1f",
						// distanceIndex, counts[distanceIndex], startAngle,
						// leftTop, bottomRight));
					} else {
						sectorPaint = background;
					}
					canvas.drawArc(new RectF(leftTop, leftTop, bottomRight, bottomRight), startAngle, halfSectorSize * 2.0f, true,
							sectorPaint);
				}
			}

			for (int sectorIndex = 0; sectorIndex < alarmStatus.getSectorCount(); sectorIndex++) {
				double bearing = alarmStatus.getSectorBearing(sectorIndex);
				canvas.drawLine(center, center, center + (float) (radius * Math.sin((bearing + halfSectorSize) / 180.0 * Math.PI)), center
						+ (float) (radius * -Math.cos((bearing + halfSectorSize) / 180.0 * Math.PI)), lines);

				String text = alarmStatus.getSectorLabel(sectorIndex);
				if (!text.equals("O")) {
					float textRadius = (AlarmSector.getDistanceLimitCount() - 0.5f) * radiusIncrement;
					canvas.drawText(text, center
							+ (float) (textRadius * Math.sin(bearing / 180.0 * Math.PI)), center
							+ (float) (textRadius * -Math.cos(bearing / 180.0 * Math.PI)) + textStyle.getFontMetrics(null) / 3f, textStyle);
				}
			}

			textStyle.setTextAlign(Align.RIGHT);
			for (int distanceIndex = 0; distanceIndex < AlarmSector.getDistanceLimitCount(); distanceIndex++) {
				float leftTop = center - (distanceIndex + 1) * radiusIncrement;
				float bottomRight = center + (distanceIndex + 1) * radiusIncrement;
				canvas.drawArc(new RectF(leftTop, leftTop, bottomRight, bottomRight), 0, 360, false, lines);

				String text = String.format("%.0f", AlarmSector.getDistanceLimits()[distanceIndex] / 1000);
				canvas.drawText(text, center + (float) (distanceIndex + 0.95f) * radiusIncrement, center
						+ textStyle.getFontMetrics(null) / 3f, textStyle);
			}

		} else {
			Log.v("AlarmView", "onDraw() alarmStatus is not set");
			
			Paint warnText = new Paint();
			warnText.setColor(0xffa00000);
			warnText.setTextAlign(Align.CENTER);
			warnText.setTextSize(20);
			warnText.setAntiAlias(true);
			
			String text = getContext().getString(R.string.alarms_not_available);
			String textLines[] = text.split("\n");
			for (int line=0; line < textLines.length; line++) {
			  canvas.drawText(textLines[line], center, center + (line - 1) * warnText.getFontMetrics(null), warnText);
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

}
